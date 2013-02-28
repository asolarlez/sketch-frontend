package sketch.compiler.passes.preprocessing;


import java.util.*;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.NameResolver;
import sketch.compiler.ast.core.Package;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.SymbolTable;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.ExprArrayInit;
import sketch.compiler.ast.core.exprs.ExprField;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprUnary;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.exprs.regens.ExprRegen;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.stmts.StmtEmpty;
import sketch.compiler.ast.core.stmts.StmtFunDecl;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypeFunction;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.ast.core.typs.TypeStruct;
import sketch.compiler.ast.core.typs.TypeStructRef;
import sketch.compiler.passes.annotations.CompilerPassDeps;
import sketch.compiler.passes.lowering.SymbolTableVisitor;
import sketch.compiler.passes.structure.CallGraph;
import sketch.util.exceptions.ExceptionAtNode;
import sketch.util.exceptions.TypeErrorException;

@CompilerPassDeps(runsBefore = {}, runsAfter = {})
public class RemoveFunctionParameters extends FEReplacer {
    static final class ParamInfo {
        final Type pt;
        // whether this variable has been changed
        // should be ORed when merging
        boolean changed;

        // the variables that this param depends on
        // currently capture the type relation
        // example: int [x] y;
        // then dependence of y contains x
        final TreeSet<String> dependence;

        public ParamInfo(Type pt, boolean changed, TreeSet<String> dependence) {
            if (pt instanceof TypeStruct) {
                pt = new TypeStructRef(((TypeStruct) pt).getFullName());
            }
            this.pt = pt;
            this.changed = changed;
            this.dependence = dependence;
        }

        @Override
        public ParamInfo clone() {
            return new ParamInfo(this.pt, this.changed,
                    (TreeSet<String>) this.dependence.clone());
        }
    }

    /**
     * Information about functions created as a result of hoisting out an inner function.
     * 
     * @author asolar
     */
    class NewFunInfo {
        public final String funName;
        public final String containingFunction;

        public final HashMap<String, ParamInfo> paramsToAdd;

        public HashMap<String, ParamInfo> cloneParamsToAdd() {
            HashMap<String, ParamInfo> c = new HashMap<String, ParamInfo>();
            for (Map.Entry<String, ParamInfo> e : paramsToAdd.entrySet()) {
                c.put(e.getKey(), e.getValue().clone());
            }
            return c;
        }

        NewFunInfo(String funName, String containingFunction) {
            this.funName = funName;
            this.containingFunction = containingFunction;
            paramsToAdd = new HashMap<String, ParamInfo>();
        }
    }

    Map<String, NewFunInfo> extractedInnerFuns =
            new HashMap<String, RemoveFunctionParameters.NewFunInfo>();
    Map<String, List<String>> equivalences = new HashMap<String, List<String>>();
    Map<String, String> reverseEquiv = new HashMap<String, String>();
    final TempVarGen varGen;

    public RemoveFunctionParameters(TempVarGen varGen) {
        this.varGen = varGen;
    }

    class ThreadClosure extends FEReplacer {
        // funName => (varName => varInfo)
        Map<String, HashMap<String, ParamInfo>> funsToVisit =
                new HashMap<String, HashMap<String, ParamInfo>>();
        Map<String, List<Parameter>> addedParams = new HashMap<String, List<Parameter>>();

        public Object visitProgram(Program prog){
            CallGraph cg = new CallGraph(prog);
            nres = new NameResolver(prog);
            for(Map.Entry<String, NewFunInfo> eif : extractedInnerFuns.entrySet() ){
                String key = eif.getKey();
                NewFunInfo nfi = eif.getValue();
                Set<String> visited = new HashSet<String>();
                Stack<String> toVisit = new Stack<String>(); 
                if(equivalences.containsKey(key)){
                    for(String fn : equivalences.get(key)){
                        toVisit.push(fn);
                        funsToVisit.put(fn, nfi.cloneParamsToAdd());
                    }
                }else{
                    toVisit.push(key);
                    funsToVisit.put(key, nfi.cloneParamsToAdd());
                }
                while(!toVisit.isEmpty()){
                    String cur = toVisit.pop();
                    if (visited.contains(cur)) {
                        continue;
                    }
                    visited.add(cur);
                    Set<Function> callers = cg.callersTo(nres.getFun(cur));
                    for (Function caller : callers) {

                        String callerName = nres.getFunName(caller);
                        String callerOriName = callerName;
                        if (reverseEquiv.containsKey(callerName)) {
                            callerOriName = reverseEquiv.get(callerName);
                        }
                        if (!callerOriName.equals(nfi.containingFunction)) {
                            toVisit.push(callerName);
                            if (funsToVisit.containsKey(callerName)) {
                                // funsToVisit.get(callerName).addAll(nfi.paramsToAdd);
                                // should merge correctly
                                HashMap<String, ParamInfo> c =
                                        funsToVisit.get(callerName);
                                for (Map.Entry<String, ParamInfo> e : nfi.paramsToAdd.entrySet())
                                {
                                    String var = e.getKey();
                                    ParamInfo info = e.getValue();
                                    ParamInfo merger = c.get(var);
                                    if (merger == null) {
                                        c.put(var, info.clone());
                                    } else {
                                        assert info.pt.equals(merger.pt);
                                        merger.changed |= info.changed;
                                        merger.dependence.addAll(info.dependence);
                                    }
                                }
                            } else {
                                funsToVisit.put(callerName, nfi.cloneParamsToAdd());
                            }
                        }
                    }
                }
                
            }
            return super.visitProgram(prog);
        }

        private List<Parameter> getAddedParams(String funName) {
            List<Parameter> result = addedParams.get(funName);
            if (result == null) {
                HashMap<String, ParamInfo> params = funsToVisit.get(funName);
                HashMap<String, Integer> indeg = new HashMap<String, Integer>();
                HashMap<String, List<String>> outedge =
                        new HashMap<String, List<String>>();
                Queue<String> readyToPut = new ArrayDeque<String>(params.size());

                for (Map.Entry<String, ParamInfo> entry : params.entrySet()) {
                    String dependent = entry.getKey();
                    Set<String> dependence = entry.getValue().dependence;
                    indeg.put(dependent, dependence.size());
                    if (dependence.size() == 0) {
                        readyToPut.add(dependent);
                    }
                    for (String var : dependence) {
                        List<String> e = outedge.get(var);
                        if (e == null) {
                            e = new ArrayList<String>();
                            outedge.put(var, e);
                        }
                        e.add(dependent);
                    }
                }

                result = new ArrayList<Parameter>();
                while (!readyToPut.isEmpty()) {
                    String name = readyToPut.remove();
                    List<String> e = outedge.get(name);
                    if (e != null) {
                        for (String dependent : e) {
                            int deg = indeg.get(dependent);
                            if (deg == 1) {
                                readyToPut.add(dependent);
                            } else {
                                indeg.put(dependent, deg - 1);
                            }
                        }
                    }
                    ParamInfo info = params.get(name);
                    result.add(new Parameter(null, info.pt, name,
                            info.changed ? Parameter.REF
                            : Parameter.IN));

                }

                addedParams.put(funName, result);
            }
            return result;
        }

        public Object visitExprFunCall(ExprFunCall efc) {
            String name = nres.getFunName(efc.getName());
            if (funsToVisit.containsKey(name)) {
                List<Expression> pl = new ArrayList<Expression>(efc.getParams());
                for (Parameter p : getAddedParams(name)) {
                    pl.add(new ExprVar(efc, p.getName()));
                }
                efc = new ExprFunCall(efc, efc.getName(), pl);
            }
            return super.visitExprFunCall(efc);
        }

        public Object visitFunction(Function fun) {
            String name = nres.getFunName(fun.getName());
            if (funsToVisit.containsKey(name)) {
                List<Parameter> pl = new ArrayList<Parameter>(fun.getParams());
                pl.addAll(getAddedParams(name));

                fun = fun.creator().params(pl).create();
            }
            return super.visitFunction(fun);
        }

    }

    class InnerFunReplacer extends SymbolTableVisitor {

        boolean isGenerator = false;

        int nfcnt = 0;
        FunReplMap frmap = new FunReplMap(null);

        InnerFunReplacer() {
            super(null);
        }
        class FunReplMap {
            FunReplMap parent = null;
            Map<String, String> frmap = new HashMap<String, String>();

            FunReplMap(FunReplMap parent) {
                this.parent = parent;
            }

            String findRepl(String old) {
                if (frmap.containsKey(old))
                    return frmap.get(old);
                if (parent != null) {
                    return parent.findRepl(old);
                }
                return null;
            }

            void declRepl(String old, String notold) {
                frmap.put(old, notold);
            }
        }

        Function curFun;
        public Object visitFunction(Function fun) {
            boolean tmpIsGen = isGenerator;
            isGenerator = fun.isGenerator();
            FunReplMap tmp = frmap;
            frmap = new FunReplMap(tmp);
            Function tmpf = curFun;
            curFun = fun;
            for (Parameter p : fun.getParams()) {

                frmap.declRepl(p.getName(), null);

            }
            Object o = super.visitFunction(fun);
            curFun = tmpf;
            frmap = tmp;
            isGenerator = tmpIsGen;
            return o;
        }

        public Object visitStmtVarDecl(StmtVarDecl svd) {
            final InnerFunReplacer localIFR = this; 
            final TempVarGen varGen = RemoveFunctionParameters.this.varGen;
            FEReplacer remREGinDecl = new FEReplacer() {
                public Object visitTypeArray(TypeArray t){
                    Type nbase = (Type)t.getBase().accept(this);
                    Expression nlen = null;
                    if (t.getLength() != null) {
                        if(t.getLength() instanceof ExprRegen){
                            String nname = varGen.nextVar();
                            localIFR.addStatement((Statement) (new StmtVarDecl(
                                    t.getLength(), TypePrimitive.inttype, nname,
                                    t.getLength())).accept(localIFR));
                            nlen = new ExprVar(t.getLength(), nname);
                        }else{
                            nlen = t.getLength();
                        }
                    }
                    if(nbase == t.getBase() &&  t.getLength() == nlen ) return t;
                    return new TypeArray(nbase, nlen, t.getMaxlength());
                }
            };

            for (int i = 0; i < svd.getNumVars(); ++i) {
                frmap.declRepl(svd.getName(i), null);
            }
            List<Type> newTypes = new ArrayList<Type>();
            boolean changed = false;

            for (int i = 0; i < svd.getNumVars(); i++) {

                Type ot = svd.getType(i);
                Type t = (Type) ot.accept(remREGinDecl);
                if (ot != t) {
                    changed = true;
                }
                newTypes.add(t);
            }
            if (!changed) {
                return super.visitStmtVarDecl(svd);
            }
            return super.visitStmtVarDecl(new StmtVarDecl(svd, newTypes, svd.getNames(),
                    svd.getInits()));

        }

        public Object visitExprFunCall(ExprFunCall efc) {
            String oldName = efc.getName();
            String newName = frmap.findRepl(oldName);
            if (newName == null) {
                newName = oldName;
            }

            List<Expression> actuals = new ArrayList<Expression>();
            for (Expression actual : efc.getParams()) {
                    String nm = frmap.findRepl(actual.toString());
                    if (nm == null) {
                        actuals.add((Expression) actual.accept(this));
                    } else {
                        actuals.add(new ExprVar(actual, nm));
                    }
            }
            return new ExprFunCall(efc, newName, actuals);
        }

        public Object visitStmtBlock(StmtBlock stmt) {
            FunReplMap tmp = frmap;
            frmap = new FunReplMap(tmp);
            Object o = super.visitStmtBlock(stmt);
            frmap = tmp;
            return o;
        }

        NewFunInfo funInfo(Function f) {
            final NewFunInfo nfi =
                    new NewFunInfo(nres.getFunName(f.getName()),
                            nres.getFunName(curFun.getName()));
            SymbolTableVisitor stv = new SymbolTableVisitor(null) {
                boolean isAssignee = false;
                TreeSet<String> dependent = null;
                public Object visitExprVar(ExprVar exp) {
                    final String name = exp.getName();

                    if (dependent != null) {
                        dependent.add(name);
                    }
                    Type t = symtab.lookupVarNocheck(exp);
                    if (t == null) {
                        // if t is not null,
                        // it's local to stmtblock (thus should not be considered
                        // closure-passed variable that's defined outside the inner
                        // function)
                        if (nres.getFun(name) != null) {
                            if (extractedInnerFuns.containsKey(nres.getFunName(name)))
                            {
                                nres.getFun(name).accept(this);
                            }
                            return exp;
                        }
                        Type pt = InnerFunReplacer.this.symtab.lookupVar(exp);
                        if (pt instanceof TypeFunction) {
                            throw new TypeErrorException(exp.getCx().toString() +
                                    ": An inner function can not use a function parameter passed to its parent function");
                        }
                        int kind =
                                InnerFunReplacer.this.symtab.lookupKind(exp.getName(),
                                        exp);
                        if (kind == SymbolTable.KIND_GLOBAL) {
                            return exp;
                        }

                        TreeSet<String> oldDependent = dependent;
                        ParamInfo info = nfi.paramsToAdd.get(name);
                        if (info == null) {
                            dependent = new TreeSet<String>();
                            nfi.paramsToAdd.put(name, new ParamInfo(pt, isAssignee,
                                    dependent));
                        } else {
                            dependent = info.dependence;
                            if (isAssignee) {
                                info.changed = true;
                            }
                        }
                        // we should also visit the type of the variable.
                        boolean oldIsA = isAssignee;
                        isAssignee = false;
                        pt.accept(this);
                        isAssignee = oldIsA;
                        dependent = oldDependent;      
                    }
                    return exp;
                }

                public Object visitTypeStruct(TypeStruct ts) {
                    return ts;
                }

                public Object visitExprField(ExprField ef) {
                    // TODO xzl: field should not be considered assignee?
                    boolean oldIsA = isAssignee;
                    isAssignee = false;
                    ef.getLeft().accept(this);
                    isAssignee = oldIsA;
                    return ef;
                }

                public Object visitExprUnary(ExprUnary exp) {
                    int op = exp.getOp();
                    if (op == ExprUnary.UNOP_POSTDEC || op == ExprUnary.UNOP_POSTINC ||
                            op == ExprUnary.UNOP_PREDEC || op == ExprUnary.UNOP_PREINC)
                    {
                        boolean oldIsA = isAssignee;
                        isAssignee = true;
                        exp.getExpr().accept(this);
                        isAssignee = oldIsA;
                    }
                    return exp;
                }

                public Object visitExprArrayInit(ExprArrayInit init) {
                    boolean oldIsA = isAssignee;
                    isAssignee = false;
                    super.visitExprArrayInit(init);
                    isAssignee = oldIsA;
                    return init;
                }

                public Object visitStmtAssign(StmtAssign stmt) {
                    boolean oldIsA = isAssignee;
                    isAssignee = true;
                    stmt.getLHS().accept(this);
                    isAssignee = oldIsA;
                    stmt.getRHS().accept(this);
                    return stmt;
                }

                public Object visitExprFunCall(ExprFunCall efc) {
                    final String name = efc.getName();
                    Function fun = nres.getFun(name);
                    if (extractedInnerFuns.containsKey(nres.getFunName(name))) {
                        // This is necessary, it's essentially inlining every inner
                        // function
                        // if you have f(...) { g(...) { } ; h(...) { ... g() ... } }
                        // g will be inlined to h
                        // why is this reasonable? because you cannot have cyclic relation
                        // (there's strict order for inner function)
                        // you might ask, later will will propagate information along call
                        // edges
                        // why do we need this?
                        // because the propagate takes advantage of this to simplify
                        // the initial condition
                        // it always put extractedInner[fun].nfi as the start point
                        // rather than the joined result
                        // in the above example, when propagate h's information,
                        // it always start from extractedInner[h].nfi
                        // but not extractedInner[h].nfi JOIN extractedInner[g].nfi
                        // and this requires g() already inlined inside h()
                        fun.accept(this);
                    }
                    // return super.visitExprFunCall(efc);
                    if (fun == null) {
                        Type t = this.symtab.lookupVar(efc.getName(), efc);
                        if (t == null || (!(t instanceof TypeFunction))) {
                            throw new ExceptionAtNode("Function " + efc.getName() +
                                " has not been defined when used", efc);
                        }
                        List<Expression> existingArgs = efc.getParams();
                        final boolean oldIsA = isAssignee;
                        for (Expression e : existingArgs) {
                            isAssignee = true;
                            e.accept(this);
                            isAssignee = oldIsA;
                        }
                        return efc;
                    }
                    List<Expression> existingArgs = efc.getParams();
                    List<Parameter> params = fun.getParams();
                    int starti = 0;
                    if (params.size() != existingArgs.size()) {
                        while (starti < params.size()) {
                            if (!params.get(starti).isImplicit()) {
                                break;
                            }
                            ++starti;
                        }
                    }

                    if ((params.size() - starti) != existingArgs.size()) {
                        throw new ExceptionAtNode("Wrong number of parameters", efc);
                    }
                    final boolean oldIsA = isAssignee;
                    for (int i = starti; i < params.size(); i++) {
                        Parameter p = params.get(i);
                        isAssignee = p.isParameterOutput();
                        existingArgs.get(i - starti).accept(this);
                        isAssignee = oldIsA;
                    }
                    return efc;
                }
            };
            stv.setNres(nres);
            f.accept(stv);
            return nfi;
        }

        public Object visitStmtFunDecl(StmtFunDecl sfd) {
            String pkg = nres.curPkg().getName();
            String oldName = sfd.getDecl().getName();
            String newName = oldName + (++nfcnt);
            while (nres.getFun(newName) != null) {
                newName = oldName + (++nfcnt);
            }
            frmap.declRepl(sfd.getDecl().getName(), newName);
            Function f = sfd.getDecl();

            if (isGenerator && !f.isGenerator()) {
                throw new ExceptionAtNode(
                        "You can not define a non-generator function inside a generator",
                        sfd);
            }
            if (f.isSketchHarness()) {
                throw new ExceptionAtNode(
                        "You can not define a harness inside another function", sfd);
            }

            Function newFun = f.creator().name(newName).pkg(pkg).create();
            nres.registerFun(newFun);
            newFun = (Function) newFun.accept(this);
            newFuncs.add(newFun);
            NewFunInfo nfi = funInfo(newFun);
            extractedInnerFuns.put(nfi.funName, nfi);
            return null;
        }
    }


    Map<String, Function> funToReplace = new HashMap<String, Function>();

    Stack<String> funsToVisit = new Stack<String>();
    Map<String, Function> newFunctions = new HashMap<String, Function>();
    Set<String> visited = new HashSet<String>();
    Map<String, Package> pkges;
    private void checkFunParameters(Function fun) {
        for(Parameter p : fun.getParams()){
            if(p.getType() instanceof TypeFunction){
                funToReplace.put(nres.getFunName(fun.getName()), fun);
                break;
            }
        }
    }
    
    public Object visitProgram(Program p) {
        p = (Program) p.accept(new InnerFunReplacer());
        nres = new NameResolver(p);

        for (Package pkg : p.getPackages()) {
            nres.setPackage(pkg);
            Set<String> nameChk = new HashSet<String>();
            for (Function fun : pkg.getFuncs()) {
                checkFunParameters(fun);
                if (nameChk.contains(fun.getName())) {
                    throw new ExceptionAtNode("Duplicated Name in Package", fun);
                }
                nameChk.add(fun.getName());
                if (fun.isSketchHarness()) {
                    funsToVisit.add(nres.getFunName(fun.getName()));
                }
                if (fun.getSpecification() != null) {
                    String spec = nres.getFunName(fun.getSpecification());
                    if (spec == null)
                        throw new ExceptionAtNode("spec of " + fun.getName() +
                                " is null. did you put the wrong name?", fun);

                    funsToVisit.add(spec);
                    funsToVisit.add(nres.getFunName(fun.getName()));
                }
            }
        }

        Map<String, List<Function>> nflistMap = new HashMap<String, List<Function>>();
        pkges = new HashMap<String, Package>();
        for (Package pkg : p.getPackages()) {
            nflistMap.put(pkg.getName(), new ArrayList<Function>());
            pkges.put(pkg.getName(), pkg);
        }

        while (!funsToVisit.isEmpty()) {
            String fname = funsToVisit.pop();
            String pkgName = getPkgName(fname);
            nres.setPackage(pkges.get(pkgName));
            Function next = nres.getFun(fname);
            if (!visited.contains(fname)) {
                Function nf = (Function) next.accept(this);
                visited.add(fname);
                nflistMap.get(pkgName).add(nf);
            }
        }
        List<Package> newPkges = new ArrayList<Package>();
        for (Package pkg : p.getPackages()) {
            newPkges.add(new Package(pkg, pkg.getName(), pkg.getStructs(),
                    pkg.getVars(), nflistMap.get(pkg.getName())));
        }
        Program np = p.creator().streams(newPkges).create();
        return np.accept(new ThreadClosure());

    }

    String getPkgName(String fname) {
        int i = fname.indexOf("@");
        return fname.substring(i + 1);
    }

    String getNameSufix(String fname) {
        int i = fname.indexOf("@");
        return fname.substring(0, i >= 0 ? i : fname.length());
    }

    public Object visitStreamSpec(Package spec)
    {
        return null;
    }

    Map<String, String> nfnMemoize = new HashMap<String, String>();
    String newFunName(ExprFunCall efc, Function orig) {
        String name = orig.getName();
        if (efc.getParams().size() != orig.getParams().size()) {
            throw new TypeErrorException(efc.getCx() +
                    "Incorrect number of parameters to function " + orig);
        }
        Iterator<Expression> fp = efc.getParams().iterator();
        for (Parameter p : orig.getParams()) {
            Expression actual = fp.next();
            if (p.getType() instanceof TypeFunction) {
                name += "_" + actual.toString();
            }
        }

        String oldName = name;
        String newName = name;
        if (nfnMemoize.containsKey(oldName)) {
            return nfnMemoize.get(oldName);
        }
        while (nres.getFun(newName) != null) {
            newName = oldName + (++nfcnt);
        }
        nfnMemoize.put(oldName, newName);
        return newName;
    }

    int nfcnt = 0;

    void addEquivalence(String old, String newName) {
        if (!equivalences.containsKey(old)) {
            equivalences.put(old, new ArrayList<String>());
        }
        equivalences.get(old).add(newName);
        reverseEquiv.put(newName, old);
    }

    ExprFunCall replaceCall(ExprFunCall efc, Function orig, String nfn) {
        List<Expression> params = new ArrayList<Expression>();
        Iterator<Expression> fp = efc.getParams().iterator();
        for (Parameter p : orig.getParams()) {
            Expression actual = fp.next();
            if (!(p.getType() instanceof TypeFunction)) {
                params.add(doExpression(actual));
            }
        }
        return new ExprFunCall(efc, nfn, params);
    }

    Function createCall(final ExprFunCall efc, Function orig, final String nfn) {
        final Map<String, String> rmap = new HashMap<String, String>();
        final String cpkg = nres.curPkg().getName();

        FEReplacer renamer = new FEReplacer() {

            public Object visitFunction(Function func)
            {


                List<Parameter> newParam = new ArrayList<Parameter>();
                
                boolean samePars = true;
                Iterator<Expression> fp = efc.getParams().iterator();
                for(Parameter par : func.getParams()){                    
                    Expression actual = fp.next();
                    Parameter newPar = (Parameter) par.accept(this) ;
                    if(!(par.getType() instanceof TypeFunction)){
                        if(par != newPar) samePars = false;
                        newParam.add( newPar );
                    }else{
                        samePars = false;
                        rmap.put(par.getName(), actual.toString());
                    }
                }

                Type rtype = (Type)func.getReturnType().accept(this);

                if( func.getBody() == null  ){
                    assert func.isUninterp() : "Only uninterpreted functions are allowed to have null bodies.";
                    if (samePars && rtype == func.getReturnType())
                        return func;
                    return func.creator().returnType(rtype).pkg(cpkg).params(newParam).create();
                }
                Statement newBody = (Statement)func.getBody().accept(this);        
                if(newBody == null) newBody = new StmtEmpty(func);
                if (newBody == func.getBody() && samePars && rtype == func.getReturnType()) return func;
                return func.creator().returnType(rtype).params(newParam).body(newBody).name(
                        nfn).pkg(cpkg).create();
            }

            public Object visitExprFunCall(ExprFunCall efc) {
                boolean hasChanged = false;
                List<Expression> newParams = new ArrayList<Expression>();
                for (Expression param : efc.getParams()) {
                    Expression newParam = doExpression(param);
                    newParams.add(newParam);
                    if (param != newParam)
                        hasChanged = true;
                }
                if (rmap.containsKey(efc.getName())) {
                    return new ExprFunCall(efc, rmap.get(efc.getName()), newParams);
                } else {
                    if (hasChanged) {
                        return new ExprFunCall(efc, efc.getName(), newParams);
                    } else {
                        return efc;
                    }
                }
            }

            public Object visitExprVar(ExprVar ev) {
                if (rmap.containsKey(ev.getName())) {
                    return new ExprVar(ev, rmap.get(ev.getName()));
                } else {
                    return ev;
                }
            }
        };

        return (Function) orig.accept(renamer);
    }

    public Object visitStmtVarDecl(StmtVarDecl svd) {
        for (int i = 0; i < svd.getNumVars(); ++i) {
            if (svd.getType(i) instanceof TypeFunction) {
                throw new ExceptionAtNode(
                        "You can not declare a variable with fun type.", svd);
            }
        }

        Object o = super.visitStmtVarDecl(svd);

        return o;
    }

    public Object visitExprFunCall(ExprFunCall efc) {

        if (efc.getName().equals("minimize")) {
            return super.visitExprFunCall(efc);
        }

        String name = nres.getFunName(efc.getName());
        if (name == null) {
            throw new ExceptionAtNode("Function " + efc.getName() +
                    " either does not exist, or is ambiguous.", efc);
        }
        if (funToReplace.containsKey(name)) {
            Function orig = funToReplace.get(name);
            String nfn = newFunName(efc, orig);
            if (newFunctions.containsKey(nfn)) {
                return replaceCall(efc, orig, nfn);
            } else {
                Function newFun = createCall(efc, orig, getNameSufix(nfn));
                nres.registerFun(newFun);
                String newName = nres.getFunName(newFun.getName());
                addEquivalence(name, newName);
                newFunctions.put(newName, newFun);
                funsToVisit.push(newName);
                return replaceCall(efc, orig, nfn);
            }
        } else {
            if (!visited.contains(name)) {
                String pkgName = getPkgName(name);
                if (pkges != null && pkges.get(pkgName) == null) {
                    throw new ExceptionAtNode("Package named " + pkgName +
                            " does not exist.", efc);
                }
                if (nres.getFun(name) == null) {
                    throw new ExceptionAtNode("Function " + efc.getName() +
                            " either does not exist, or is ambiguous.", efc);
                }
                funsToVisit.push(name);
            }
            return super.visitExprFunCall(efc);
        }
    }
    
}

