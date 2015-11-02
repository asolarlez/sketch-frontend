package sketch.compiler.dataflow.nodesToSB;

import java.io.PrintStream;
import java.util.*;

import sketch.compiler.ast.core.Annotation;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.NameResolver;
import sketch.compiler.ast.core.Package;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprFunCall;
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

        public int compare(SpecSketch o1, SpecSketch o2) {
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
        combineFunCalls = true;
        this.tracing = tracing;
        if(tracing){
            rcontrol.activateTracing();
        }
        this.maxArrSize = maxArrSize;
        state.useRetTracker();
    }

    private String convertType(Type type) {
        // This is So Wrong in the greater scheme of things.
        if (type instanceof TypeArray)
        {
            TypeArray array = (TypeArray)type;
            String base = convertType(array.getBase());
            abstractValue iv = (abstractValue) array.getLength().accept(this);          
            return base + "[" + iv + "]";
        }
 else if (type instanceof TypeStructRef)
        {
            return ((TypeStructRef) type).getName();
        }
        else if (type instanceof TypePrimitive)
        {
            switch (((TypePrimitive)type).getType())
            {
                case TypePrimitive.TYPE_BIT:
                    return "bit";
                case TypePrimitive.TYPE_INT:
                    return "int";
                case TypePrimitive.TYPE_FLOAT:
                    return "float";
                case TypePrimitive.TYPE_DOUBLE:
                    return "double";
                case TypePrimitive.TYPE_VOID:
                    return "void";
            }
        }
        return null;
    }

    List<Integer> opsizes;
    String finalOpname;
    boolean hasOutput;

    private boolean visitingALen=false;



    public Object visitTypeArray(TypeArray t) {
        String extra = " here base " + t.getBase() + " len " + t.getLength();
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

    public void doParams(List<Parameter> params, List<String> addInitStmts) {
        PrintStream out = ((NtsbVtype)this.vtype).out;
        boolean first = true;
        out.print("(");
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
                            state.setVarValue(
                                    lhs,
                                    (abstractValue) ta.getBase().defaultValue().accept(
                                            this));
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
                return struct.getName().toUpperCase() + "_" +
                        struct.getPkg().toUpperCase();
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
                return struct.getName().toUpperCase() + "_" +
                        struct.getPkg().toUpperCase();
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
        doParams(func.getParams(), initStmts);

        PrintStream out = ((NtsbVtype) this.vtype).out;
        out.println("{");
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
            out.print(finalOpname + "= [" + func.getName().toUpperCase() + "_" +
                    func.getPkg().toUpperCase() + "]{< ");
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
        out.println("typedef{");
        nres = new NameResolver(p);
        for (Package pkg : p.getPackages()) {
            nres.setPackage(pkg);
            for (StructDef t : pkg.getStructs()) {
                if (t.immutable()) {
                    out.print(t.getName().toUpperCase() + "_" + t.getPkg().toUpperCase() +
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
            }
        }
        for (Package pkg : p.getPackages()) {
            nres.setPackage(pkg);
            for (Function fun : pkg.getFuncs()) {
                if (fun.getPkg() == null) {
                    out.print(fun.getName().toUpperCase() + "_ANNONYMOUS" + " ( ");
                } else {
                    out.print(fun.getName().toUpperCase() + "_" +
                            fun.getPkg().toUpperCase() +
                      " ( ");
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

        Object o = super.visitProgram(p);
        for (Package pkg : p.getPackages()) {
            for (StmtSpAssert sa : pkg.getSpAsserts()) {
                ((NtsbVtype) this.vtype).out.println("replace " + printSpAssert(sa) + ";");
            }
        }


        SpecSketchComparator cp = new SpecSketchComparator();
        cp.estimate(assertions);
        Collections.sort(assertions, cp);

        for (SpecSketch s : assertions) {
            ((NtsbVtype) this.vtype).out.println("assert " + s + ";");
        }

        return o;
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
