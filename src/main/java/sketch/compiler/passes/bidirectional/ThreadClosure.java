package sketch.compiler.passes.bidirectional;

import java.util.*;
import java.util.Map.Entry;

import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.NameResolver;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.passes.structure.CallGraph;
import sketch.compiler.stencilSK.VarReplacer;

/**
 * This is the very last step: after all inner functions have been hoisted
 * out and function params removed, we need to pass around variables that
 * are used in inner functions. "Thread" the closures.
 * 
 * @author asolar
 */
public class ThreadClosure extends FEReplacer {
    /**
     * 
     */
    private final RemoveFunctionParameters removeFunctionParameters;
    private final InnerFunReplacer ifreplacer;

    /**
     * @param removeFunctionParameters
     */
    public ThreadClosure(RemoveFunctionParameters removeFunctionParameters, InnerFunReplacer ifreplacer) {
        this.removeFunctionParameters = removeFunctionParameters;
        this.ifreplacer = ifreplacer;
    }

    // funName => (varName => varInfo)
    Map<String, Map<String, ParamInfo>> funsToVisit = new HashMap<String, Map<String, ParamInfo>>();
    Map<String, List<Parameter>> addedParams = new HashMap<String, List<Parameter>>();

    Set<String> enclosingAdded;

    Map<String, ParamInfo> mergePI(Map<String, ParamInfo> lhs, Map<String, ParamInfo> rhs) {
        for (Map.Entry<String, ParamInfo> e : rhs.entrySet()) {
            String var = e.getValue().name;
            ParamInfo calleeInfo = e.getValue();
            ParamInfo callerInfo = lhs.get(var);
            if (callerInfo == null) {
                boolean renameInType = false;
                Map<String, Expression> repl = renamesFromDeps(calleeInfo.dependence, lhs, rhs);
                if (repl.size() > 0) {
                    VarReplacer vr = new VarReplacer(repl);
                    lhs.put(var, calleeInfo.clone(vr).makePassthrough());
                } else {
                    lhs.put(var, calleeInfo.clone().makePassthrough());
                }
            } else {
                if (callerInfo.uniqueName().equals(calleeInfo.uniqueName())) {
                    assert calleeInfo.paramType.equals(callerInfo.paramType);
                    callerInfo.changed |= calleeInfo.changed;
                    callerInfo.dependence.addAll(calleeInfo.dependence);
                } else {
                    // Everyone who depents on me has
                    // already had their types fixed, so
                    // its ok.
                    Map<String, Expression> repl = renamesFromDeps(calleeInfo.dependence, lhs, rhs);
                    if (repl.size() > 0) {
                        lhs.put(e.getValue().uniqueName, calleeInfo.clone(new VarReplacer(repl)).makePassthrough());
                    } else {
                        lhs.put(e.getValue().uniqueName, calleeInfo.clone().makePassthrough());
                    }
                }
            }
        }
        return lhs;
    }

    Map<String, Expression> renamesFromDeps(TreeSet<String> dependence, Map<String, ParamInfo> pinfosOfCaller, Map<String, ParamInfo> pinfosOfCallee) {
        Map<String, Expression> repl = new HashMap<String, Expression>();

        for (String dep : dependence) {
            ParamInfo localDep = pinfosOfCaller.get(dep);
            ParamInfo passthroughDep = pinfosOfCallee.get(dep);
            if (localDep != null) {
                if (!passthroughDep.uniqueName.equals(localDep.uniqueName)) {
                    repl.put(dep, new ExprVar((FEContext) null, passthroughDep.uniqueName));
                }
            }
        }
        return repl;
    }

    public Object visitProgram(Program prog) {
        CallGraph cg = new CallGraph(prog);
        nres = new NameResolver(prog);
        for (Map.Entry<String, NewFunInfo> eif : this.ifreplacer.extractedInnerFuns.entrySet()) {
            String key = eif.getKey();
            NewFunInfo nfi = eif.getValue();
            Set<String> visited = new HashSet<String>();
            Stack<String> toVisit = new Stack<String>();
            if (this.removeFunctionParameters.equivalences.containsKey(key)) {
                for (String fn : this.removeFunctionParameters.equivalences.get(key)) {
                    toVisit.push(fn);
                    if (funsToVisit.containsKey(fn)) {
                        funsToVisit.put(fn, mergePI(nfi.cloneParamsToAdd(), funsToVisit.get(fn)));
                    } else {
                        funsToVisit.put(fn, nfi.cloneParamsToAdd());
                    }
                }
            } else {
                toVisit.push(key);
                if (funsToVisit.containsKey(key)) {
                    funsToVisit.put(key, mergePI(nfi.cloneParamsToAdd(), funsToVisit.get(key)));
                } else {
                    funsToVisit.put(key, nfi.cloneParamsToAdd());
                }
            }
            while (!toVisit.isEmpty()) {
                String cur = toVisit.pop();
                if (visited.contains(cur)) {
                    continue;
                }
                visited.add(cur);
                Set<Function> callers = cg.callersTo(nres.getFun(cur));
                for (Function caller : callers) {
                    String callerName = nres.getFunName(caller);
                    String callerOriName = callerName;
                    if (this.removeFunctionParameters.reverseEquiv.containsKey(callerName)) {
                        callerOriName = this.removeFunctionParameters.reverseEquiv.get(callerName);
                    }
                    if (!callerName.equals(nfi.containingFunction)) {
                        toVisit.push(callerName);
                        if (funsToVisit.containsKey(callerName)) {
                            // funsToVisit.get(callerName).addAll(nfi.paramsToAdd);
                            // should merge correctly
                            Map<String, ParamInfo> pinfosOfCaller = funsToVisit.get(callerName);
                            mergePI(pinfosOfCaller, nfi.paramsToAdd);
                        } else {
                            // Get the current function
                            Function currentFunction = cg.getByName(cur);


                            funsToVisit.put(callerName, renameParams(nfi.cloneParamsToAdd()));
                        }
                    }
                }
            }

        }
        return super.visitProgram(prog);
    }

    HashMap<String, ParamInfo> renameParams(HashMap<String, ParamInfo> torename) {
        HashMap<String, ParamInfo> newmap = new HashMap<String, ParamInfo>();
        for (Entry<String, ParamInfo> entry : torename.entrySet()) {
            newmap.put(entry.getValue().name, entry.getValue().makePassthrough());
        }
        return newmap;
    }

    private List<Parameter> getAddedParams(String funName, boolean isGenerator, boolean isCall) {
        List<Parameter> result = null; // addedParams.get(funName);
        if (result == null) {
            Map<String, ParamInfo> params = funsToVisit.get(funName);
            HashMap<String, Integer> indeg = new HashMap<String, Integer>();
            HashMap<String, List<String>> outedge = new HashMap<String, List<String>>();
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

            Map<String, Expression> vmap = new HashMap<String, Expression>();
            FEReplacer repl = new VarReplacer(vmap);
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
                boolean makeRef = info.changed;
                if (isGenerator) {
                    makeRef = makeRef || info.paramType instanceof TypeArray;
                }
                String tname = name;
                if (isCall) {
                    if (enclosingAdded.contains(info.uniqueName())) {
                        tname = info.uniqueName();
                    } else {
                        tname = info.name;
                    }

                } else {
                    if (info.isPassthrough()) {
                        tname = info.uniqueName();
                        vmap.put(name, new ExprVar((FEContext) null, info.uniqueName()));
                    } else {
                        tname = name;
                    }
                }
                result.add(new Parameter(null, (Type) info.paramType.accept(repl), tname, makeRef ? Parameter.REF : Parameter.IN));

            }

            addedParams.put(funName, result);
        }
        return result;
    }

    /**
     * Visit the parameter to check if it needs to be renamed to another
     * name. This happens when a lambda that needs a value threaded calls an
     * inner function
     */
    public Object visitParameter(Parameter parameter, Function function) {
        // Check the super visitor
        return super.visitParameter(parameter);
    }

    public Object visitExprFunCall(ExprFunCall efc) {
        String name = nres.getFunName(efc.getName());
        Function f = nres.getFun(efc.getName());
        if (funsToVisit.containsKey(name)) {
            List<Parameter> addedParams = getAddedParams(name, f.isGenerator(), true);
            if (addedParams.size() != 0) {
                List<Expression> pl = new ArrayList<Expression>(efc.getParams());
                for (Parameter p : addedParams) {
                    pl.add(new ExprVar(efc, p.getName()));
                }
                efc = new ExprFunCall(efc, efc.getName(), pl);
            }
        }
        return super.visitExprFunCall(efc);
    }

    public Object visitFunction(Function fun) {
        String name = nres.getFunName(fun.getName());
        enclosingAdded = new HashSet<String>();
        if (funsToVisit.containsKey(name)) {
            List<Parameter> pl = new ArrayList<Parameter>(fun.getParams());
            List<Parameter> newps = getAddedParams(name, fun.isGenerator(), false);
            for (Parameter p : newps) {
                p = (Parameter) this.visitParameter(p, fun);
                enclosingAdded.add(p.getName());
                pl.add(p);
            }
            // pl.addAll(newps);

            fun = fun.creator().params(pl).create();
        }
        return super.visitFunction(fun);
    }

} // end of ThreadClosure