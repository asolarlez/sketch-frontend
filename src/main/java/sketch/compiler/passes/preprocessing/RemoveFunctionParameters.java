package sketch.compiler.passes.preprocessing;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.NameResolver;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.StreamSpec;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.stmts.StmtEmpty;
import sketch.compiler.ast.core.stmts.StmtFunDecl;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeFunction;
import sketch.compiler.passes.annotations.CompilerPassDeps;
import sketch.compiler.passes.lowering.SymbolTableVisitor;
import sketch.compiler.passes.structure.CallGraph;
import sketch.util.exceptions.TypeErrorException;
import sketch.util.exceptions.UnrecognizedVariableException;

@CompilerPassDeps(runsBefore = {}, runsAfter = {})
public class RemoveFunctionParameters extends FEReplacer {
    static final class ParamInfo {
        final Type pt;

        // the variables that this param depends on
        final TreeSet<String> dependence;
 
        public ParamInfo(Type pt, TreeSet<String> dependence) {
            this.pt = pt;
            this.dependence = dependence;
        }

        @Override
        public ParamInfo clone() {
            return new ParamInfo(this.pt, (TreeSet<String>) this.dependence.clone());
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

    class ThreadClosure extends FEReplacer {
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
                                for (Map.Entry<String, ParamInfo> e : nfi.paramsToAdd.entrySet()) {
                                    String var = e.getKey();
                                    ParamInfo info = e.getValue();
                                    ParamInfo merger = c.get(var);
                                    if (merger == null) {
                                        c.put(var, info.clone());
                                    } else {
                                        assert info.pt.equals(merger.pt);
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
                Queue<String> readyToPut = new ArrayBlockingQueue<String>(params.size());

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
                    result.add(new Parameter(params.get(name).pt, name, Parameter.REF));
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
            return o;
        }

        public Object visitStmtVarDecl(StmtVarDecl svd) {
            for (int i = 0; i < svd.getNumVars(); ++i) {
                frmap.declRepl(svd.getName(i), null);
            }
            return super.visitStmtVarDecl(svd);
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
                TreeSet<String> dependent = null;
                public Object visitExprVar(ExprVar exp) {
                    // we should also visit the type of the variable.
                    if (dependent != null) {
                        dependent.add(exp.getName());
                    }
                    Type t = symtab.lookupVarNocheck(exp);
                    if (t == null) {
                        if (nres.getFun(exp.getName()) != null) {
                            if (extractedInnerFuns.containsKey(nres.getFunName(exp.getName())))
                            {
                                nres.getFun(exp.getName()).accept(this);
                            }
                            return exp;
                        }
                        Type pt = InnerFunReplacer.this.symtab.lookupVar(exp);
                        if (pt instanceof TypeFunction) {
                            throw new TypeErrorException(exp.getCx().toString() +
                                    ": An inner function can not use a function parameter passed to its parent function");
                        }
                        String name = exp.getName();
                        ParamInfo info = nfi.paramsToAdd.get(name);
                        TreeSet<String> oldDependent = dependent;
                        if (info == null) {
                            dependent = new TreeSet<String>();
                            nfi.paramsToAdd.put(name, new ParamInfo(pt, dependent));
                        } else {
                            dependent = info.dependence;
                        }

                        pt.accept(this);
                        dependent = oldDependent;
                    }
                    return exp;
                }

                public Object visitExprFunCall(ExprFunCall efc) {
                    if (extractedInnerFuns.containsKey(nres.getFunName(efc.getName()))) {
                        nres.getFun(efc.getName()).accept(this);
                    }
                    return super.visitExprFunCall(efc);
                }

            };
            stv.setNres(nres);
            f.accept(stv);
            return nfi;
        }

        public Object visitStmtFunDecl(StmtFunDecl sfd) {
            String newName = sfd.getDecl().getName() + (++nfcnt);
            frmap.declRepl(sfd.getDecl().getName(), newName);
            Function f = sfd.getDecl();
            Function newFun = f.creator().name(newName).create();
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
        for (StreamSpec pkg : p.getStreams()) {
            nres.setPackage(pkg);
            for (Function fun : pkg.getFuncs()) {
                checkFunParameters(fun);
                if (fun.isSketchHarness()) {
                    funsToVisit.add(nres.getFunName(fun.getName()));
                }
                if (fun.getSpecification() != null) {
                    funsToVisit.add(nres.getFunName(fun.getSpecification()));
                    funsToVisit.add(nres.getFunName(fun.getName()));
                }
            }
        }

        Map<String, List<Function>> nflistMap = new HashMap<String, List<Function>>();
        Map<String, StreamSpec> pkges = new HashMap<String, StreamSpec>();
        for (StreamSpec pkg : p.getStreams()) {
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
        List<StreamSpec> newPkges = new ArrayList<StreamSpec>();
        for (StreamSpec pkg : p.getStreams()) {
            newPkges.add(new StreamSpec(pkg, pkg.getName(), pkg.getStructs(),
                    pkg.getVars(), nflistMap.get(pkg.getName())));
        }
        return p.creator().streams(newPkges).create().accept(new ThreadClosure());

    }

    String getPkgName(String fname) {
        int i = fname.indexOf(":");
        return fname.substring(0, i);
    }

    String getNameSufix(String fname) {
        int i = fname.indexOf(":");
        return fname.substring(i + 1);
    }

    public Object visitStreamSpec(StreamSpec spec)
    {
        return null;
    }

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
        return name;
    }

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
                    return func.creator().returnType(rtype).params(newParam).create();
                }
                Statement newBody = (Statement)func.getBody().accept(this);        
                if(newBody == null) newBody = new StmtEmpty(func);
                if (newBody == func.getBody() && samePars && rtype == func.getReturnType()) return func;
                return func.creator().returnType(rtype).params(newParam).body(newBody).name(nfn).create();
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


    public Object visitExprFunCall(ExprFunCall efc) {

        String name = nres.getFunName(efc.getName());
        if (name == null) {
            throw new UnrecognizedVariableException(efc.getName(), efc);
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
                funsToVisit.push(name);
            }
            return super.visitExprFunCall(efc);
        }
    }
    
}
