package sketch.compiler.dataflow.nodesToSB;

import java.io.PrintStream;
import java.util.*;
import java.util.Map.Entry;

import sketch.compiler.ast.core.Annotation;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.NameResolver;
import sketch.compiler.ast.core.Package;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprField;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprStar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssert;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtIfThen;
import sketch.compiler.ast.core.stmts.StmtReturn;
import sketch.compiler.ast.core.stmts.StmtSpAssert;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.StructDef;
import sketch.compiler.ast.core.typs.StructDef.StructFieldEnt;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.ast.core.typs.TypeStructRef;
import sketch.compiler.ast.cuda.exprs.CudaThreadIdx;
import sketch.compiler.ast.cuda.stmts.CudaSyncthreads;
import sketch.compiler.dataflow.MethodState.Level;
import sketch.compiler.dataflow.PartialEvaluator;
import sketch.compiler.dataflow.abstractValue;
import sketch.compiler.dataflow.recursionCtrl.RecursionControl;
import sketch.compiler.main.cmdline.SketchOptions;
import sketch.compiler.solvers.constructs.ValueOracle;
import sketch.util.exceptions.ExceptionAtNode;
/**
 * This class translates the ast into a boolean function which is output to a file.
 * The format is suitable for the SBitII backend solver.<BR>
 * Preconditions:
 * <DL>
 * <DT>  Arrays
 * <DD>  * Only 1 dimensional arrays allowed.
 * <DD>  * The [] operator can only be used on an array variable.
 * <DD>  * binary operators work only for scalars.
 * <DD>  * Array to array assignments are supported. (arr1 = arr2)
 * <DD>  * Array ranges with len != 1 are not supported.
 * <DD>  * Scalar to array assignments are supported.
 * </DL>
 * 
 * @author asolar
 */
public class ProduceBooleanFunctions extends PartialEvaluator {
    boolean tracing = false;
    int maxArrSize;

    class FunctionHoleTracker {
        Function current;
        Map<String, Set<String>> functionHoles = new HashMap<String, Set<String>>();
        Map<String, Set<String>> functionCalls = new HashMap<String, Set<String>>();
        Map<String, Set<String>> fixes = new HashMap<String, Set<String>>();

        public void enterFunction(Function current) {
            this.current = current;
            assert !functionHoles.containsKey(current.getName());
            functionHoles.put(current.getFullName(), new HashSet<String>());
            functionCalls.put(current.getFullName(), new HashSet<String>());
            recordFixes(current);
        }

        public void registerUninterp(Function current) {
            assert !functionHoles.containsKey(current.getName());
            functionHoles.put(current.getFullName(), new HashSet<String>());
            functionCalls.put(current.getFullName(), new HashSet<String>());
        }

        public Set<String> getFixes(String name) {
            if (fixes.containsKey(name)) {
                return fixes.get(name);
            }
            return null;
        }

        public void recordFixes(Function f) {
            fixes.put(f.getFullName(), new HashSet<String>(f.getFixes()));
        }

        public void regCall(ExprFunCall efc) {
            functionCalls.get(current.getFullName()).add(nres.getFunName(efc.getName()));
        }

        public void regHole(ExprStar hole) {
            functionHoles.get(current.getFullName()).add(hole.getSname());
        }

        public Set<String> holesForFun(String fun) {
            return functionHoles.get(fun);
        }

        public void computeFixpoint() {
            boolean changed = false;
            do {
                changed = false;
                for (Entry<String, Set<String>> fhole : functionHoles.entrySet()) {
                    Set<String> holes = fhole.getValue();
                    String fun = fhole.getKey();
                    Set<String> callees = functionCalls.get(fun);
                    for (String callee : callees) {
                        int sz = holes.size();
                        holes.addAll(functionHoles.get(callee));
                        changed = changed || (sz != holes.size());
                    }
                }
            } while (changed);
        }

    }

    FunctionHoleTracker fhtrack = new FunctionHoleTracker();

    class SpecSketch {
        public final String spec;
        public final String sketch;

        SpecSketch(String spec, String sketch) {
            this.spec = spec;
            this.sketch = sketch;

            this.stmt_cnt = 0;
            this.callees = new ArrayList<Function>();
        }

        public String toString(){
            return sketch + " SKETCHES " + spec;
        }

        // inexact yet quick estimate for the size of this function
        int stmt_cnt;

        protected void incStmtCnt() {
            this.stmt_cnt++;
        }

        protected int getStmtCnt() {
            return this.stmt_cnt;
        }

        // simple call relations to take into accounts callees' size
        List<Function> callees;

        protected void addFunCall(Function fun) {
            this.callees.add(fun);
        }

        protected List<Function> getCallees() {
            return this.callees;
        }
    }

    class SpecSketchComparator implements Comparator<SpecSketch> {

        Map<SpecSketch, Integer> funSizes;
        SketchOptions options;

        public SpecSketchComparator() {
            funSizes = new HashMap<SpecSketch, Integer>();
            options = SketchOptions.getSingleton();
        }

        String eol = System.getProperty("line.separator");

        public void estimate(List<SpecSketch> ssks) {

            for (SpecSketch ssk : ssks) {
                StringBuffer buf = new StringBuffer();
                buf.append("estimated size of ");
                buf.append(ssk.sketch + eol);

                int sz = ssk.getStmtCnt();
                buf.append("stmt cnt: " + sz + eol);

                List<Function> callees = ssk.getCallees();
                for (Function callee : callees) {
                    // FIXME xzl: is this the correct behaviro? uninterp functions have
                    // size 0
                    Statement body = callee.getBody();
                    int callee_sz = body == null ? 0 : body.size();
                    sz += callee_sz;
                    buf.append("callee " + callee.getName() + " : " + callee_sz);
                    buf.append(eol);
                }

                if (options.debugOpts.verbosity >= 10) {
                    System.out.println(buf.toString());
                }
                funSizes.put(ssk, sz);
            }
        }

        /**
         * Positive means o1 goes after o2.
         */
        public int compare(SpecSketch o1, SpecSketch o2) {

            Set<String> fixes1 = fhtrack.getFixes(o1.sketch);
            Set<String> fixes2 = fhtrack.getFixes(o2.sketch);

            if (fixes1 != null) {
                if (fixes2 == null) {
                    return -1; // o1 has fixes, o2 does not, so o1 goes first.
                }
            }
            if (fixes2 != null) {
                if (fixes1 == null) {
                    return 1; // o2 has fixes, o1 does not, so o2 goes first.
                }
            }

            int s1 = 0;
            int s2 = 0;
            if (funSizes.containsKey(o1)) {
                s1 = funSizes.get(o1);
            }
            if (funSizes.containsKey(o2)) {
                s2 = funSizes.get(o2);
            }
            return s1 - s2;
        }
    }

    List<SpecSketch> assertions = new ArrayList<SpecSketch>();
    List<Statement> todoStmts = new ArrayList<Statement>();
    private void dischargeTodo(){
        for(Statement s : todoStmts){
            s.accept(this);
        }
        todoStmts.clear();
    }
    public ProduceBooleanFunctions(TempVarGen varGen, 
            ValueOracle oracle, PrintStream out, int maxUnroll, int maxArrSize, RecursionControl rcontrol, boolean tracing){
        super(new NtsbVtype(oracle, out), varGen, false, maxUnroll, rcontrol);
        this.tracing = tracing;
        if(tracing){
            rcontrol.activateTracing();
        }
        this.maxArrSize = maxArrSize;
        state.useRetTracker();
    }



    List<Integer> opsizes;
    String finalOpname;
    boolean hasOutput;

    private boolean visitingALen=false;

    public Object visitExprField(ExprField exp) {
        throw new RuntimeException("COMPILER BUG: There should not be any Expression Fields at this point!!!");
    }

    public Object visitExprStar(ExprStar star) {
        fhtrack.regHole(star);
        return super.visitExprStar(star);
    }


    public Object visitTypeArray(TypeArray t) {

        Type nbase = (Type)t.getBase().accept(this);
        visitingALen = true;
        abstractValue avlen = null;
        Expression nlen;
        try{
            if (t.getLength() != null) {
                avlen = (abstractValue) t.getLength().accept(this);
                nlen = this.exprRV;
            } else {
                avlen = vtype.BOTTOM("FARRAY");
                nlen = null;
            }
        }finally{
            visitingALen = false;
        }
        Expression elen = t.getLength();
        /*
         * if(avlen.isBottom()){ //nlen = maxArrSize; String msg =
         * "Arrays are not big enough" + elen + ">" + nlen; todoStmts.add(new
         * StmtAssert(new ExprBinary(elen, "<=", nlen), msg +extra, false)); }else{ nlen =
         * ExprConstInt.createConstant(avlen.getIntVal()); }
         */
        return t;
        // if(nbase == t.getBase() && t.getLength() == nlen ) return t;
        // return new TypeArray(nbase, nlen) ;
    }

    // if (formal.getType().isArray()) {
    // int maxlength = ((TypeArray) formal.getType()).getMaxlength();
    // if (maxlength > 0) {
    // return new ExprConstInt(maxlength);
    // }
    // }

    /*
     * protected Expression interpretActualParam(Expression e){ return maxArrSize; }
     */

    public Map<String, abstractValue> doParams(List<Parameter> params, List<String> addInitStmts) {
        PrintStream out = ((NtsbVtype)this.vtype).out;
        boolean first = true;
        out.print("(");
        Map<String, abstractValue> toinit = new HashMap<String, abstractValue>();
        for(Iterator<Parameter> iter = params.iterator(); iter.hasNext(); ){
            Parameter param = iter.next();
            Type ptype = (Type) param.getType().accept(this);

            if (param.isParameterOutput()) {
                hasOutput = true;
            } else {
                if (!first)
                    out.print(", ");
                first = false;
                out.print(printType(ptype) + " ");
            }
            String lhs = param.getName();

            if(param.isParameterOutput()){
                state.outVarDeclare(lhs , ptype);
            }else{
                state.varDeclare(lhs , ptype);
            }
            IntAbsValue inval = (IntAbsValue) state.varValue(lhs);
            String invalName = inval.toString();
            if( ptype instanceof TypeArray ){
                TypeArray ta = (TypeArray) ptype;
                if (inval.isVect()) {
                    IntAbsValue tmp = (IntAbsValue) ta.getLength().accept(this);
                    assert inval.isVect() : "If it is not a vector, something is really wrong.\n";
                    int sz = tmp.getIntVal();
                    if (param.isParameterOutput()) {
                        opsizes.add(sz);
                    }
                    for (int tt = 0; tt < sz; ++tt) {
                        String nnm = inval.getVectValue().get(tt).toString();
                        if (!param.isParameterOutput()) {
                            out.print(nnm + " ");
                        }
                    }
                } else {
                    if(param.isParameterOutput()){
                        opsizes.add(1);
                        // Armando: This statement below is experimental.
                        if (!param.isParameterInput()) {
                            toinit.put(lhs, (abstractValue) ta.getBase().defaultValue().accept(this));
                        }
                    }else{
                        out.print(invalName + " ");
                    }
                }
            }else{
                if(param.isParameterOutput()){
                    opsizes.add(1);

                }else{
                    out.print(invalName + " ");
                }
            }

            if (param.isParameterOutput()) {
                if (param.isParameterInput()) {
                    if (!first) out.print(", ");
                    first = false;
                    out.print(printType(ptype) + " ");
                    if (ptype instanceof TypeArray) {
                        if (inval.isVect()) {
                            TypeArray ta = (TypeArray) ptype;
                            IntAbsValue tmp = (IntAbsValue) ta.getLength().accept(this);
                            assert inval.isVect() : "If it is not a vector, something is really wrong.\n";
                            int sz = tmp.getIntVal();
                            for (int tt = 0; tt < sz; ++tt) {
                                String nnm = inval.getVectValue().get(tt).toString();
                                {
                                    out.print(nnm + " ");
                                }
                            }
                        } else {
                            out.print(invalName + " ");
                        }
                    } else {
                        out.print(invalName + " ");
                    }
                }
            }
            if (param.getSrcTupleDepth() != -1) {
                out.print("<" + param.getSrcTupleDepth() + " > ");
            }
        }
        // Add the output parameter
        if (hasOutput) {
            if (!first)
                out.print(", ");
            out.print("! ");
            out.print("NOREC ");
            out.print(finalOpname);
        }
        out.print(")");
        return toinit;
    }

    static String filterPound(String s) {
        if (s.length() > 0 && s.charAt(0) == '#') {
            return s.substring(1);
        } else {
            return s;
        }
    }

    private String printType(Type type) {
        if (type instanceof TypeArray) {
            TypeArray array = (TypeArray) type;
            String base = printType(array.getBase());
            abstractValue iv;
            if (array.getLength() != null) {
                iv = (abstractValue) array.getLength().accept(this);
            } else {
                iv = vtype.BOTTOM("FARRAY");
            }
            if (iv.isBottom()) {
                return base + "[*" + maxArrSize + "]";
            } else {
                return base + "[" + iv + "]";
            }
        }
        if (type instanceof TypeStructRef) {
            TypeStructRef ts = (TypeStructRef) type;
            StructDef struct = nres.getStruct(ts.getName());
            if (struct.immutable()) {
                return NtsbVtype.adtTypeName(struct);
            }
        }

        if (type.equals(TypePrimitive.bittype)) {
            return "bit";
        } else {
            if (type.equals(TypePrimitive.floattype) ||
                    type.equals(TypePrimitive.doubletype))
            {
                return "float";
            } else {
                return "int";
            }
        }
    }

    private String printTupleType(Type type) {
        if (type instanceof TypeArray) {
            TypeArray array = (TypeArray) type;
            String base = printType(array.getBase());
            abstractValue iv;
            if (array.getLength() instanceof ExprConstInt) {
                iv = (abstractValue) array.getLength().accept(this);
            } else {
                iv = vtype.BOTTOM("FARRAY");
            }

            return base + "[*" + maxArrSize + "]";

        }
        if (type instanceof TypeStructRef) {
            TypeStructRef ts = (TypeStructRef) type;
            StructDef struct = nres.getStruct(ts.getName());
            if (struct.immutable()) {
                NtsbVtype vt = (NtsbVtype) vtype;
                return vt.adtTypeName(struct);
            }
        }

        if (type.equals(TypePrimitive.bittype)) {
            return "bit";
        } else {
            if (type.equals(TypePrimitive.floattype) ||
                    type.equals(TypePrimitive.doubletype))
            {
                return "float";
            } else {
                return "int";
            }
        }
    }

    public void doOutParams(List<Parameter> params) {
        PrintStream out = ((NtsbVtype) this.vtype).out;
        Iterator<Integer> opsz = opsizes.iterator();
        if (hasOutput) {

            for (Iterator<Parameter> iter = params.iterator(); iter.hasNext();) {
                Parameter param = iter.next();
                String lhs = param.getName();
                if (param.isParameterOutput()) {
                    IntAbsValue inval = (IntAbsValue) state.varValue(lhs);
                    assert opsz.hasNext() : "This can't happen.";
                    int sz = opsz.next();
                    for (int tt = 0; tt < sz; ++tt) {
                        String nnm = null;
                        if (inval.isVect()) {
                            nnm = inval.getVectValue().get(tt).toString();
                        } else {
                            assert tt == 0;
                            nnm = inval.toString();
                        }
                        out.print(nnm + " ");
                    }
                }
            }
            out.println(">};");
        }
    }

    public Object visitStmtReturn(StmtReturn sr) {
        state.testReturn();
        return sr;
    }

    class inRange {
        public final int low;
        public final int high;
        public final String name;

        inRange(String annotation, Function f) {
            int ls = annotation.indexOf(':');
            int hs = annotation.indexOf(':', ls + 1);
            if (ls > 0 && ls < hs && hs < annotation.length()) {
                String lo = annotation.substring(ls + 1, hs);
                String hi = annotation.substring(hs + 1, annotation.length());
                try {
                low = Integer.decode(lo);
                high = Integer.decode(hi);
                } catch (NumberFormatException nfe) {
                    throw new ExceptionAtNode(
                            "The syntax for inrange is @inrange(\"param:lower:upper\") where lower and upper are integers.",
                            f);
                }
                name = annotation.substring(0, ls);
            } else {
                throw new ExceptionAtNode(
                        "The syntax for inrange is @inrange(\"param:lower:upper\")", f);
            }

        }

    }

    boolean isMain(Function func) {
        return mainfuns.contains(func.getName());
    }

    SpecSketch currentFun;

    public Object visitFunction(Function func)
    {
        fhtrack.enterFunction(func);

        if (func.hasAnnotation("DontAnalyze")) {
            return func;
        }



        if(tracing)
            System.out.println("Analyzing " + func.getName() + " " + new java.util.Date());

        if (func.isModel()) {
            ((NtsbVtype) this.vtype).out.print("mdl_def " + func.getName());
        } else {
            ((NtsbVtype) this.vtype).out.print("def " + func.getName());
        }

        if (func.getSpecification() != null) {
            SpecSketch ssk = new SpecSketch(func.getSpecification(), func.getName());
            assertions.add(ssk);
            currentFun = ssk;
        } else {
            // to not accumulate stmt cnt in a wrong place
            currentFun = null;
        }

        List<Integer> tmpopsz = opsizes;
        String tmpOpname = finalOpname;
        boolean tmpHasOutput = hasOutput;

        opsizes = new ArrayList<Integer>();
        finalOpname = "_p_out_" + func.getName() + "_" + func.getPkg();
        hasOutput = false;

        Level lvl = state.beginFunction(func.getName());
        List<String> initStmts = new ArrayList<String>();
        Map<String, abstractValue> toinit = doParams(func.getParams(), initStmts);

        PrintStream out = ((NtsbVtype) this.vtype).out;
        out.println("{");
        for (Entry<String, abstractValue> toi : toinit.entrySet()) {
            state.setVarValue(toi.getKey(), toi.getValue());
        }

        if (func.isWrapper()) {
            String wname = func.getName();
            int ix = 0;
            do {
                ix = wname.indexOf("__Wrapper", 0);
            } while (wname.indexOf("__Wrapper", ix + 1) >= 0);
            String hname = func.getName().substring(0, ix);
            Function tfun = nres.getFun(hname);
            int ii = 0;
            while (tfun == null && ii < 10) {
                tfun = nres.getFun(hname + ii);
                ++ii;
            }
            if (tfun != null) {
                Vector<Annotation> annot = tfun.getAnnotation("inrange");
                for (Annotation a : annot) {
                    inRange ir = new inRange(a.contents(), tfun);
                    String pname = "";
                    boolean found = false;
                    Parameter thep = null;
                    for (Parameter p : func.getParams()) {
                        pname = p.getName();
                        if (pname.length() > ir.name.length()) {
                            if (pname.substring(0, ir.name.length()).equals(ir.name)) {
                                found = true;
                                thep = p;
                                break;
                            }
                        }
                    }
                    if (!found) {
                        throw new ExceptionAtNode("Parameter " + ir.name +
                                " from annotation " + a + " not found.", func);
                    }
                    if (!thep.getType().equals(TypePrimitive.inttype)) {
                        throw new ExceptionAtNode(
                                "inrange annotation only allowed for int parameters: " +
                                        a, func);
                    }
                    abstractValue av = state.varValue(pname);
                    state.setVarValue(pname, vtype.plus(av, vtype.CONST(ir.low)));
                    vtype.Assume(vtype.le(state.varValue(pname), vtype.CONST(ir.high)),
                            null);

                }
            }
        } else {
            /*
             * Vector<Annotation> annot = func.getAnnotation("inrange"); if (annot.size()
             * > 0 && !func.isSketchHarness()) { throw new ExceptionAtNode(
             * "@inrange annotation only allowed in harness functions.", func); }
             */
        }
        

        
        
        for (String s : initStmts) {
            out.println(s);
        }
        dischargeTodo();
        Statement newBody = (Statement) func.getBody().accept(this);

        state.handleReturnTrackers();
        if (hasOutput) {
            out.print(finalOpname + "= [" + NtsbVtype.funTypeName(func) + "]{< ");
        }
        doOutParams(func.getParams());

        state.endFunction(lvl);
        out.println("}");

        opsizes = tmpopsz;
        hasOutput = tmpHasOutput;
        finalOpname = tmpOpname;

        if (tracing)
            System.out.println("Analyzed " + func.getName() + " " + new java.util.Date());

        return func;
    }

    Set<String> mainfuns = new HashSet<String>();

    public Object visitProgram(Program p) {
        PrintStream out = ((NtsbVtype) this.vtype).out;
        printDeclarationsAndRegisterMainfuns(p, out);

        Object o = super.visitProgram(p);
        for (Package pkg : p.getPackages()) {
            for (StmtSpAssert sa : pkg.getSpAsserts()) {
                ((NtsbVtype) this.vtype).out.println("replace " + printSpAssert(sa) + ";");
            }
        }

        fhtrack.computeFixpoint();
        SpecSketchComparator cp = new SpecSketchComparator();
        cp.estimate(assertions);
        Collections.sort(assertions, cp);


        for (SpecSketch s : assertions) {
            Function sk = nres.getFun(s.sketch);
            String assrt = "assert " + s;
            if (sk.hasAnnotation("FromFile")) {
                Vector<Annotation> anot = sk.getAnnotation("FromFile");
                assrt += " FILE ";
                for (Annotation an : anot) {
                    assrt += "\"" + an.contents() + "\"";
                }
            }
            ((NtsbVtype) this.vtype).out.println(assrt + ";");
            String sname = nres.getFunName(s.sketch);
            Set<String> fixes = fhtrack.getFixes(sname);
            if (fixes != null && fixes.size() > 0) {
                for(String tofix : fixes){
                    Set<String> holes = fhtrack.holesForFun(tofix);
                    for (String hole : holes) {
                        ((NtsbVtype) this.vtype).out.println("fixhole " + hole + ";");
                    }
                }
            }
        }

        return o;
    }



    private void printDeclarationsAndRegisterMainfuns(Program p, PrintStream out) {
        

        out.println("typedef{");
        nres = new NameResolver(p);
        for (Package pkg : p.getPackages()) {
            nres.setPackage(pkg);
            for (StructDef t : pkg.getStructs()) {
                if (t.immutable()) {
                    out.print(NtsbVtype.adtTypeName(t) +
                            " ( ");
                    int actFields = t.getActFields();
                    if (actFields <= 0)
                        actFields = t.getNumFields();
                    out.print(actFields + " ");
                    for (StructFieldEnt e : t.getFieldEntriesInOrder()) {
                        out.print(printTupleType(e.getType()) + " ");
                    }
                    out.println(")");
                }
            }
            for (Function f : pkg.getFuncs()) {
                if (f.getSpecification() != null) {
                    mainfuns.add(f.getName());
                    mainfuns.add(f.getSpecification());
                }
                if (f.isUninterp()) {
                    fhtrack.registerUninterp(f);
                }
            }
        }
        for (Package pkg : p.getPackages()) {
            nres.setPackage(pkg);
            for (Function fun : pkg.getFuncs()) {

                if (fun.isUninterp() && fun.hasAnnotation("Gen")) {
                    out.print("_GEN_" + fun.getAnnotation("Gen").get(0).contents() + "(");
                } else {
                    out.print(NtsbVtype.funTypeName(fun) + " ( ");
                }
                List<Parameter> params = fun.getParams();
                for (Iterator<Parameter> iter = params.iterator(); iter.hasNext();) {
                    Parameter param = iter.next();
                    if (param.isParameterOutput()) {
                        if (param.getType().isArray()) {
                            TypeArray ta = (TypeArray) param.getType();
                            Expression el = ta.getLength();
                            Integer lntt = el != null ? el.getIValue() : null;
                            if (lntt != null) {
                                int lnt = lntt;
                                for (int i = 0; i < lnt; ++i) {
                                    out.print(printType(ta.getBase()) + " ");
                                }

                            } else {
                                Type base = ta.getBase();
                                if (base.isStruct()) {
                                    out.print(printType(ta.getBase()) + "[*" +
                                            maxArrSize + "]" + " ");
                                } else {
                                    out.print(printType(ta.getBase()) + "_arr" + " ");
                                }
                            }
                        } else {
                            out.print(printType(param.getType()) + " ");
                        }
                    }
                }
                out.println(")");
                
            }
        }
        out.println("}");
    }

    private String printSpAssert(StmtSpAssert sa) {
        String res = "";
        ExprFunCall f1 = sa.getFirstFun();
        res += f1.getName();
        for (Expression param : f1.getParams()) {
            if (param instanceof ExprFunCall) {
                ExprFunCall pa = (ExprFunCall) param;
                res += " * ";
                res += pa.getName();
            }
        }

        res += " EQUALS ";

        ExprFunCall f2 = sa.getSecondFun();
        res += f2.getName();
        for (Expression param : f2.getParams()) {
            if (param instanceof ExprFunCall) {
                ExprFunCall pa = (ExprFunCall) param;
                res += " * ";
                res += pa.getName();
            }
        }

        res += " (" + sa.getStateCount() + ")";

        return res;
    }
    public Object visitExprFunCall(ExprFunCall exp) {
        fhtrack.regCall(exp);
        String name = exp.getName();
        // Local function?
        Function fun = nres.getFun(name);
        if (currentFun != null) {
            currentFun.addFunCall(fun);
        }
        String funPkg = fun.getPkg();
        if (fun.getSpecification() != null) {
            assert false : "The substitution of sketches for their respective specs should have been done in a previous pass.";
        }
        if (fun != null) {
            if (fun.isUninterp()) {
                Object o = super.visitExprFunCall(exp);
                dischargeTodo();
                return o;
            } else {
                if (rcontrol.testCall(exp)) {
                    /* Increment inline counter. */
                    rcontrol.pushFunCall(exp, fun);

                    List<Statement> oldNewStatements = newStatements;
                    newStatements = new ArrayList<Statement>();
                    Level lvl2 = state.pushFunCall(exp.getName());
                    try {
                        Level lvl;
                        {
                            Iterator<Expression> actualParams =
                                    exp.getParams().iterator();
                            Iterator<Parameter> formalParams = fun.getParams().iterator();
                            lvl =
                                    inParameterSetter(exp, formalParams, actualParams,
                                            false);
                        }
                        Statement body = null;
                        try {

                            body = (Statement) fun.getBody().accept(this);
                        } catch (RuntimeException ex) {
                            state.popLevel(lvl); // This is to compensate for a pushLevel
                            // in inParamSetter.
                            // Under normal circumstances, this gets offset by a popLevel
                            // in outParamSetter, but not in the pressence of exceptions.
                            throw ex;
                        }
                        addStatement(body);
                        {
                            Iterator<Expression> actualParams =
                                    exp.getParams().iterator();
                            Iterator<Parameter> formalParams = fun.getParams().iterator();
                            outParameterSetter(formalParams, actualParams, false, lvl);
                        }
                    } finally {
                        state.popFunCall(lvl2);
                        newStatements = oldNewStatements;
                    }
                    rcontrol.popFunCall(exp);
                } else {
                    if (rcontrol.leaveCallsBehind()) {
                        // System.out.println("        Stopped recursion:  " +
                        // fun.getName());
                        funcsToAnalyze.add(fun);
                        Object o = super.visitExprFunCall(exp);
                        dischargeTodo();
                        return o;
                    } else {
                        StmtAssert sas = new StmtAssert(exp, ExprConstInt.zero, false);
                        sas.setMsg((exp != null ? exp.getCx().toString() : "") +
                                exp.getName());
                        sas.accept(this);
                    }
                }
                exprRV = exp;
                return vtype.BOTTOM();
            }
        }
        exprRV = null;
        return vtype.BOTTOM();
    }

    private Object tmp = null;

    @Override
    public Object visitStmtIfThen(StmtIfThen s) {
        if (tracing) {
            // if(s.getCx() != tmp && s.getCx() != null){
            abstractValue av = (abstractValue) s.getCond().accept(this);
            System.out.println(s.getCx() + " : \t cond(" + s.getCond() + ")   " + av);
            tmp = s.getCx();
            // }
        }
        return super.visitStmtIfThen(s);
    }

    public Object visitStmtVarDecl(StmtVarDecl s) {
        if (tracing) {
            // if(s.getCx() != tmp && s.getCx() != null){

            tmp = s.getCx();
            if (s.getNumVars() == 1 && s.getInit(0) != null) {
                abstractValue av = (abstractValue) s.getInit(0).accept(this);
                System.out.println(s.getCx() + " : \t" + s + "\t rhs=" + av);
            } else {
                System.out.println(s.getCx() + " : \t" + s);
            }
            // }
        }
        Object o = super.visitStmtVarDecl(s);
        dischargeTodo();
        return o;
    }

    public Object visitStmtAssert(StmtAssert sa) {
        if (currentFun != null) {
            currentFun.incStmtCnt();
        }
        return super.visitStmtAssert(sa);
    }

    public Object visitStructDef(StructDef ts) {
        return ts;
    }

    public Object visitStmtAssign(StmtAssign s) {
        if (tracing) {
            // if(s.getCx() != tmp && s.getCx() != null){
            abstractValue av = (abstractValue) s.getRHS().accept(this);
            System.out.println(s.getCx() + " : \t" + s + "\t rhs=" + av);
            tmp = s.getCx();
            // }
        }
        if (currentFun != null) {
            currentFun.incStmtCnt();
        }
        return super.visitStmtAssign(s);
    }


    @Override
    public Object visitCudaThreadIdx(CudaThreadIdx cudaThreadIdx) {
        throw new ExceptionAtNode("Cuda threadIdx should be erased by now! "
                + "Did you forget to add a \"device\" qualifier to the function?",
                cudaThreadIdx);
    }

    @Override
    public Object visitCudaSyncthreads(CudaSyncthreads cudaSyncthreads) {
        throw new ExceptionAtNode("Cuda __syncthreads() should be erased by now! "
                + "Did you forget to add a \"device\" qualifier to the function?",
                cudaSyncthreads);
    }
}
