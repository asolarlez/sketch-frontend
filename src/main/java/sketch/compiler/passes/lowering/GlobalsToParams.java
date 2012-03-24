package sketch.compiler.passes.lowering;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Vector;

import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.FieldDecl;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Function.FcnType;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.StreamSpec;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.stmts.StmtExpr;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.cuda.typs.CudaMemoryType;
import sketch.compiler.passes.annotations.CompilerPassDeps;
import sketch.compiler.passes.structure.CallGraph;
import sketch.compiler.passes.structure.CallGraph.CallEdge;
import sketch.util.datastructures.HashmapSet;
import sketch.util.datastructures.TypedHashMap;
import sketch.util.datastructures.TypedHashSet;

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
    protected final TypedHashMap<String, Function> glblInitFcns =
            new TypedHashMap<String, Function>();
    protected final TypedHashSet<Function> fcnsToAdd = new TypedHashSet<Function>();
    protected Function enclosingFcn;

    public GlobalsToParams(TempVarGen varGen) {
        this.varGen = varGen;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Entry<Function, HashMap<String, AddedParam>> v : newParamsForCall.entrySet())
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
        prog.accept(this.fldNames);
        prog.accept(this.glblExprs);

        // add all base expressions
        for (Entry<Function, HashSet<String>> ent : this.glblExprs.globalVarRefs.entrySet())
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
            final HashMap<String, AddedParam> callerParams =
                    newParamsForCall.getCreate(caller);
            final HashMap<String, AddedParam> calleeParams =
                    newParamsForCall.getCreate(callee);
            for (AddedParam calleeParam : calleeParams.values()) {
                if (!callerParams.containsKey(calleeParam.globalVar)) {
                    callerParams.put(calleeParam.globalVar, new AddedParam(
                            calleeParam.globalVar, calleeParam.typ));
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
        // System.err.println(this.toString());
        // prog.debugDump();
        // System.exit(0);
        return prog;
    }

    @Override
    public Object visitStreamSpec(StreamSpec spec) {
        spec = (StreamSpec) super.visitStreamSpec(spec);
        final Vector<Function> fcns = new Vector<Function>(spec.getFuncs());
        for (Function fcn : this.fcnsToAdd) {
            fcns.add(fcn);
        }
        this.fcnsToAdd.clear();
        return new StreamSpec(spec, spec.getName(), spec.getStructs(),
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
            if (!callGraph.isSketchOrSpec(inputFcn)) {
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

    @Override
    public Object visitExprVar(ExprVar exp) {
        if (fldNames.hasName(exp.getName()) && (enclosingFcn != null)) {
            final HashMap<String, AddedParam> fcnGlbls =
                    newParamsForCall.get(enclosingFcn);
            assert fcnGlbls != null : "no key for function " + enclosingFcn;
            final AddedParam paramvar = fcnGlbls.get(exp.getName());
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
        Parameter outvar = new Parameter(type, tmpName, Parameter.OUT);
        params.add(outvar);
        StmtAssign assign = new StmtAssign(new ExprVar(ctx, tmpName), expression);

        StmtBlock body = new StmtBlock(assign);
        return Function.creator(ctx, varGen.nextVar("glblInit_" + glblName),
                FcnType.Static).params(params).body(body).create();
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
            newParams.add(new Parameter(param.typ, param.paramName, Parameter.REF));
        }
        return newParams;
    }

    /** find the names of global variables */
    public class GlobalFieldNames extends FEReplacer {
        private final TypedHashSet<String> fieldNames = new TypedHashSet<String>();
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
            this.getFieldNames().addAll(field.getNames());
            fieldTypes.addZipped(field.getNames(), field.getTypes());
            fieldInits.addZipped(field.getNames(), field.getInits());
            return field;
        }

        public boolean hasName(String name) {
            if (shadows.contains(name))
                return false;
            return fieldNames.contains(name);
        }

        public AddedParam createParam(String globalVar) {
            return new AddedParam(globalVar, fieldTypes.get(globalVar));
        }

        public TypedHashSet<String> getFieldNames() {
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
        HashmapSet<Function, String> globalVarRefs = new HashmapSet<Function, String>();
        protected Function enclosing;

        @Override
        public Object visitFunction(Function func) {
            this.enclosing = func;
            return super.visitFunction(func);
        }

        @Override
        public Object visitExprVar(ExprVar exp) {
            if (fldNames.getFieldNames().contains(exp.getName())) {
                // System.err.println("adding global var ref " + exp.getName());
                globalVarRefs.add(enclosing, exp.getName());
            }
            return super.visitExprVar(exp);
        }
    }

    public static class FcnToParamsMap extends
            TypedHashMap<Function, HashMap<String, AddedParam>>
    {
        @Override
        public HashMap<String, AddedParam> createValue() {
            return new HashMap<String, AddedParam>();
        }
    }
}
