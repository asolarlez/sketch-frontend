package sketch.compiler.passes.lowering;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import sketch.compiler.ast.core.*;
import sketch.compiler.ast.core.Function.FcnType;
import sketch.compiler.ast.core.Package;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.stmts.StmtExpr;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeStructRef;
import sketch.compiler.ast.cuda.typs.CudaMemoryType;
import sketch.compiler.passes.annotations.CompilerPassDeps;
import sketch.compiler.passes.structure.CallGraph;
import sketch.compiler.passes.structure.CallGraph.CallEdge;
import sketch.util.datastructures.OrderedHashSet;
import sketch.util.datastructures.TreemapSet;
import sketch.util.datastructures.TypedHashMap;
import sketch.util.datastructures.TypedTreeMap;
import sketch.util.exceptions.ExceptionAtNode;

/**
 * convert global variables to inout parameters, with a static initializer function.
 * deletes all global variables afterwards.
 * 
 * <pre>
 * int G = 4;
 * int G2 = ??;
 * 
 * void fcn()
 *      x = G;
 *      G = ??;
 * </pre>
 * 
 * to
 * 
 * <pre>
 * void fcn(ref G)
 * 
 * void getGInitial() { return G; }
 * 
 * main implements ...
 *     G = getGInitial()
 *     fcn(G)
 * </pre>
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
@CompilerPassDeps(runsAfter = {}, runsBefore = {})
public class GlobalsToParams extends FEReplacer {
    protected CallGraph callGraph;
    protected GlobalFieldNames fldNames;
    protected FcnToParamsMap newParamsForCall = new FcnToParamsMap();
    protected GlobalExprs glblExprs;
    protected final TempVarGen varGen;
    protected final TypedTreeMap<String, Function> glblInitFcns =
            new TypedTreeMap<String, Function>();
    protected final OrderedHashSet<Function> fcnsToAdd = new OrderedHashSet<Function>();
    protected Function enclosingFcn;

    public GlobalsToParams(TempVarGen varGen) {
        this.varGen = varGen;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Entry<Function, TreeMap<String, AddedParam>> v : newParamsForCall.entrySet())
        {
            sb.append("=== new params for " + v.getKey().getName() + " ===\n");
            for (AddedParam param : v.getValue().values()) {
                sb.append("    glbl " + param.globalVar + " =: " + param.paramName + "\n");
            }
        }
        return sb.toString();
    }

    @Override
    public Object visitProgram(Program prog) {
        this.callGraph = new CallGraph(prog);
        // prog.debugDump();
        // System.out.println(callGraph.toString());
        // System.exit(0);
        this.fldNames = new GlobalFieldNames();
        this.glblExprs = new GlobalExprs();
        nres = new NameResolver(prog);
        prog.accept(this.fldNames);
        prog.accept(this.glblExprs);

        // add all base expressions
        for (Entry<Function, TreeSet<String>> ent : this.glblExprs.globalVarRefs.entrySet())
        {
            for (String globalVarName : ent.getValue()) {
                newParamsForCall.getCreate(ent.getKey()).put(globalVarName,
                        fldNames.createParam(globalVarName));
            }
        }

        // add all necessary params for callers (closure of above)
        for (CallEdge closureEdge : callGraph.closureEdges) {
            final Function caller = closureEdge.caller();
            final Function callee = closureEdge.target();
            final TreeMap<String, AddedParam> callerParams =
                    newParamsForCall.getCreate(caller);
            final TreeMap<String, AddedParam> calleeParams =
                    newParamsForCall.getCreate(callee);
            for (AddedParam calleeParam : calleeParams.values()) {
                if (!callerParams.containsKey(calleeParam.globalVar)) {
                    callerParams.put(calleeParam.globalVar, new AddedParam(
                            calleeParam.globalVar, calleeParam.typ));
                }
            }
        }


        // Make sure specs and sketches get the same parameters even if they don't use the
        // same globals.
        for (Package pkg : prog.getPackages()) {
            nres.setPackage(pkg);
            for (Function f : pkg.getFuncs()) {
                if (f.getSpecification() != null) {
                    Function spec = nres.getFun(f.getSpecification());

                    if (f.isWrapper() || spec.isWrapper()) {
                        continue;
                    }

                    final TreeMap<String, AddedParam> callerParams =
                            newParamsForCall.getCreate(f);
                    final TreeMap<String, AddedParam> calleeParams =
                            newParamsForCall.getCreate(spec);
                    for (AddedParam calleeParam : calleeParams.values()) {
                        if (!callerParams.containsKey(calleeParam.globalVar)) {
                            callerParams.put(calleeParam.globalVar, new AddedParam(
                                    calleeParam.globalVar, calleeParam.typ));
                        }
                    }
                    for (AddedParam callerParam : callerParams.values()) {
                        if (!calleeParams.containsKey(callerParam.globalVar)) {
                            calleeParams.put(callerParam.globalVar, new AddedParam(
                                    callerParam.globalVar, callerParam.typ));
                        }
                    }
                }
            }
        }

        // add all initialization functions
        for (String fldName : this.fldNames.getFieldNames()) {
            final Expression fieldInit = fldNames.getFieldInit(fldName);
            if (fieldInit != null) {
                final Function initFcn =
                        getInitFcn(fldName, fldNames.getType(fldName), fieldInit);
                glblInitFcns.put(fldName, initFcn);
                fcnsToAdd.add(initFcn);
            }
        }


        // replace all function calls
        // System.err.println(this);
        prog = (Program) super.visitProgram(prog);
        assert fcnsToAdd.isEmpty();
        nres = new NameResolver(prog); // get the new versions of functions into the nres.
        for (Package pkg : prog.getPackages()) {
            nres.setPackage(pkg);
            for (Function f : pkg.getFuncs()) {
                if (f.getSpecification() != null) {
                    Function spec = nres.getFun(f.getSpecification());
                    if (spec.getParams().size() != f.getParams().size()) {
                        System.out.println(spec);
                        System.out.println(f);
                        String msg =
                                "Some variables are not being updated by spec and sketch in the same way:";
                        Iterator<Parameter> p1 = spec.getParams().iterator();
                        Iterator<Parameter> p2 = f.getParams().iterator();
                        while (p1.hasNext() || p2.hasNext()) {
                            Parameter pp1 = null, pp2 = null;
                            if (p1.hasNext()) {
                                pp1 = p1.next();
                            }
                            if (p2.hasNext()) {
                                pp2 = p2.next();
                            }
                            if (pp1 == null) {
                                msg += pp2.getName() + ", ";
                            }
                            if (pp2 == null) {
                                msg += pp1.getName() + ", ";
                            }
                        }
                        throw new ExceptionAtNode(msg, f);
                    }
                }
            }
        }

        // System.err.println(this.toString());
        // prog.debugDump();
        // System.exit(0);
        return prog;
    }

    @Override
    public Object visitPackage(Package spec) {
        callGraph.getNres().setPackage(spec);
        spec = (Package) super.visitPackage(spec);
        final Vector<Function> fcns = new Vector<Function>(spec.getFuncs());
        for (Function fcn : this.fcnsToAdd) {
            fcns.add(fcn);
        }
        this.fcnsToAdd.clear();
        return new Package(spec, spec.getName(), spec.getStructs(),
                Collections.EMPTY_LIST, fcns);
    }

    @Override
    public Object visitFunction(Function inputFcn) {
        enclosingFcn = inputFcn;
        fldNames.pushBlock();
        try {
        final Function fcn = (Function) super.visitFunction(inputFcn);

        // the hashmap only contains keys for the old function.
        // NOTE -- a litte messy, please add better ideas if you have them.
        if (newParamsForCall.containsKey(inputFcn)) {
                if (!inputFcn.isWrapper()) {
                final Vector<Parameter> params = new Vector<Parameter>(fcn.getParams());

                // same here, need to look up the old function
                params.addAll(getParametersForFcn(inputFcn));
                return fcn.creator().params(params).create();
            } else {

                StmtBlock body = (StmtBlock) fcn.getBody();
                Vector<Statement> stmts = new Vector<Statement>(body.getStmts());
                for (AddedParam param : newParamsForCall.get(inputFcn).values()) {
                    // typ x;
                    stmts.insertElementAt(new StmtVarDecl(fcn, param.typ,
                            param.paramName, null), 0);

                    // init(&x)
                    if (param.hasInitCall()) {
                        ExprVar ref = new ExprVar(fcn, param.paramName);
                        stmts.insertElementAt(new StmtExpr(
                                param.getInitVarCall(body, ref)), 1);
                    }
                }
                body = new StmtBlock(stmts);
                return fcn.creator().body(body).create();
            }
        } else {
            return fcn;
        }
        } finally {
            fldNames.popBlock();
        }
    }

    public Object visitParameter(Parameter par) {
        fldNames.addShadow(par.getName());
        Object o = super.visitParameter(par);
        return o;
    }

    public Object visitStmtBlock(StmtBlock sb) {
        fldNames.pushBlock();
        try {
            Object o = super.visitStmtBlock(sb);
            return o;
        } finally {
            fldNames.popBlock();
        }
    }

    public Object visitStmtVarDecl(StmtVarDecl svd) {
        for (int i = 0; i < svd.getNumVars(); ++i) {
            fldNames.addShadow(svd.getName(i));
        }
        Object o = super.visitStmtVarDecl(svd);
        return o;
    }

    @SuppressWarnings("deprecation")
    @Override
    public Object visitExprFunCall(ExprFunCall callParam) {
        ExprFunCall call = (ExprFunCall) super.visitExprFunCall(callParam);
        Vector<Expression> fcnArgs = new Vector<Expression>(call.getParams());
        Function caller = callGraph.getEnclosing(callParam);
        Function callee = callGraph.getTarget(callParam);
        for (AddedParam param : newParamsForCall.get(callee).values()) {
            String localVarName =
                    newParamsForCall.get(caller).get(param.globalVar).paramName;
            fcnArgs.add(new ExprVar(FEContext.artificalFrom("ref-" + localVarName,
                    callParam), localVarName));
        }
        return new ExprFunCall(callParam, callParam.getName(), fcnArgs);
    }

    static String compName(String nm, String pkg) {
        return nm + "__" + pkg;
    }

    @Override
    public Object visitExprVar(ExprVar exp) {
        String pkgname = nres.curPkg().getName();
        if (fldNames.hasName(exp.getName(), pkgname) &&
                (enclosingFcn != null))
        {
            final TreeMap<String, AddedParam> fcnGlbls =
                    newParamsForCall.get(enclosingFcn);
            assert fcnGlbls != null : "no key for function " + enclosingFcn;
            final AddedParam paramvar = fcnGlbls.get(compName(exp.getName(), pkgname));
            assert paramvar != null : "no parameter variable for " + exp.getName();
            return new ExprVar(exp, paramvar.paramName);
        } else {
            return super.visitExprVar(exp);
        }
    }

    @SuppressWarnings( { "deprecation" })
    public Function getInitFcn(String glblName, Type type, Expression expression) {
        if (expression.getCx() == null) {
            assert false;
        }
        final FEContext ctx = FEContext.artificalFrom("global_init_fcn", expression);

        String tmpName = varGen.nextVar(glblName);
        Vector<Parameter> params = new Vector<Parameter>();
        Parameter outvar = new Parameter(expression, type, tmpName, Parameter.OUT);
        params.add(outvar);
        StmtAssign assign = new StmtAssign(new ExprVar(ctx, tmpName), expression);

        StmtBlock body = new StmtBlock(assign);
        return Function.creator(ctx, varGen.nextVar("glblInit_" + glblName),
                FcnType.Static).params(params).body(body).pkg(nres.curPkg().getName()).create();
    }

    public class AddedParam {
        public final String globalVar;
        public final Type typ;
        public final String paramName;

        public AddedParam(String globalVar, Type typ, String paramName) {
            this.globalVar = globalVar;
            this.typ = typ.withMemType(CudaMemoryType.GLOBAL);
            this.paramName = paramName;
        }

        public boolean hasInitCall() {
            return glblInitFcns.get(globalVar) != null;
        }

        @SuppressWarnings( { "deprecation" })
        public ExprFunCall getInitVarCall(StmtBlock ctx, ExprVar param) {
            final Function fcn = glblInitFcns.get(globalVar);
            Vector<Expression> args = new Vector<Expression>();
            args.add(param);
            return new ExprFunCall(FEContext.artificalFrom("init var call", ctx),
                    fcn.getName(), args);
        }

        public AddedParam(String globalVar, Type typ) {
            this(globalVar, typ, varGen.nextVar(globalVar));
        }
    }

    public String getTmpVarForGlobal(Function function, String globalName) {
        return newParamsForCall.get(function).get(globalName).paramName;
    }

    public Vector<Parameter> getParametersForFcn(Function fcn) {
        Vector<Parameter> newParams = new Vector<Parameter>();
        for (AddedParam param : newParamsForCall.get(fcn).values()) {
            newParams.add(new Parameter(fcn, param.typ, param.paramName,
                    Parameter.REF));
        }
        return newParams;
    }

    /** find the names of global variables */
    public class GlobalFieldNames extends FEReplacer {
        private final Set<String> fieldNames = new TreeSet<String>();
        private final TypedHashMap<String, Type> fieldTypes =
                new TypedHashMap<String, Type>();
        private final TypedHashMap<String, Expression> fieldInits =
                new TypedHashMap<String, Expression>();
        ShadowStack shadows = new ShadowStack(null);

        public void pushBlock() {
            shadows = shadows.push();
        }

        public void popBlock() {
            shadows = shadows.pop();
        }

        public void addShadow(String vname) {
            shadows.add(vname);
        }
        class ShadowStack {
            final HashSet<String> shadow = new HashSet<String>();
            final ShadowStack prev;

            ShadowStack(ShadowStack prev) {
                this.prev = prev;
            }

            ShadowStack push() {
                ShadowStack s = new ShadowStack(this);
                return s;
            }

            ShadowStack pop() {
                return this.prev;
            }

            void add(String s) {
                shadow.add(s);
            }

            boolean contains(String s) {
                if (shadow.contains(s)) {
                    return true;
                }
                if (prev != null) {
                    return prev.contains(s);
                }
                return false;
            }
        }
        @SuppressWarnings("unchecked")
        @Override
        public Object visitFieldDecl(FieldDecl field) {
            String pkg = nres.curPkg().getName();
            int i = 0;
            for (Type ft : field.getTypes()) {
                if (ft instanceof TypeStructRef) {
                    ft = ((TypeStructRef) ft).addDefaultPkg(pkg, nres);
                }
                String fldName = compName(field.getName(i), pkg);
                fieldTypes.put(fldName, ft);
                fieldInits.put(fldName, field.getInit(i));
                fieldNames.add(fldName);
                ++i;
            }

            return field;
        }

        public boolean hasName(String name, String pkg) {
            if (shadows.contains(name))
                return false;
            return fieldNames.contains(compName(name, pkg));
        }

        public AddedParam createParam(String globalVar) {
            return new AddedParam(globalVar, fieldTypes.get(globalVar));
        }

        public Set<String> getFieldNames() {
            return fieldNames;
        }

        public Type getType(String glblName) {
            return fieldTypes.get(glblName);
        }

        public Expression getFieldInit(String glblName) {
            return fieldInits.get(glblName);
        }
    }

    /** detect references to global variables, and record the enclosing functions */
    public class GlobalExprs extends FEReplacer {
        TreemapSet<Function, String> globalVarRefs = new TreemapSet<Function, String>();
        protected Function enclosing;
        String pkg;
        @Override
        public Object visitFunction(Function func) {
            this.enclosing = func;
            pkg = func.getPkg();
            return super.visitFunction(func);
        }

        @Override
        public Object visitExprVar(ExprVar exp) {

            if (fldNames.getFieldNames().contains(compName(exp.getName(), pkg))) {
                // System.err.println("adding global var ref " + exp.getName());
                globalVarRefs.add(enclosing, compName(exp.getName(), pkg));
            }
            return super.visitExprVar(exp);
        }
    }

    public static class FcnToParamsMap extends
            TypedTreeMap<Function, TreeMap<String, AddedParam>>
    {
        @Override
        public TreeMap<String, AddedParam> createValue() {
            return new TreeMap<String, AddedParam>();
        }
    }
}
