package sketch.compiler.passes.preprocessing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.StreamSpec;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtEmpty;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeFunction;
import sketch.compiler.passes.annotations.CompilerPassDeps;

@CompilerPassDeps(runsBefore = {}, runsAfter = {})
public class RemoveFunctionParameters extends FEReplacer {

    Map<String, Function> funToReplace = new HashMap<String, Function>();
    Stack<Function> funsToVisit = new Stack<Function>();
    Map<String, Function> newFunctions = new HashMap<String, Function>();
    Set<String> visited = new HashSet<String>();
    private void checkFunParameters(Function fun) {
        for(Parameter p : fun.getParams()){
            if(p.getType() instanceof TypeFunction){
                funToReplace.put(fun.getName(), fun);
                break;
            }
        }
    }
    
    public Object visitStreamSpec(StreamSpec spec)
    {
        
        StreamSpec oldSS = sspec;
        sspec = spec;
        for (Function fun : spec.getFuncs()) {
            checkFunParameters(fun);
            if (fun.isSketchHarness()) {
                funsToVisit.add(fun);
            }
            if (fun.getSpecification() != null) {
                funsToVisit.add(spec.getFuncNamed(fun.getSpecification()));
                funsToVisit.add(fun);
            }
        }

        List<Function> nflist = new ArrayList<Function>();
        while (!funsToVisit.isEmpty()) {
            Function next = funsToVisit.pop();
            if (!visited.contains(next.getName())) {
                Function nf = (Function) next.accept(this);
                visited.add(nf.getName());
                nflist.add(nf);
            }
        }
        sspec = oldSS;
        return new StreamSpec(spec, spec.getType(), spec.getName(),
                spec.getParams(), spec.getVars(), nflist);
    }

    String newFunName(ExprFunCall efc, Function orig) {
        String name = orig.getName();
        Iterator<Expression> fp = efc.getParams().iterator();
        for (Parameter p : orig.getParams()) {
            Expression actual = fp.next();
            if (p.getType() instanceof TypeFunction) {
                name += "_" + actual.toString();
            }
        }
        return name;
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
        String name = efc.getName();
        if (funToReplace.containsKey(name)) {
            Function orig = funToReplace.get(name);
            String nfn = newFunName(efc, orig);
            if (newFunctions.containsKey(nfn)) {
                return replaceCall(efc, orig, nfn);
            } else {
                Function newFun = createCall(efc, orig, nfn);
                newFunctions.put(nfn, newFun);
                funsToVisit.push(newFun);
                return replaceCall(efc, orig, nfn);
            }
        } else {
            if (!visited.contains(efc.getName())) {
                funsToVisit.push(sspec.getFuncNamed(name));
            }
            return super.visitExprFunCall(efc);
        }
    }
    
}
