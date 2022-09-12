package sketch.compiler.dataflow.preprocessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Package;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprNew;
import sketch.compiler.ast.core.exprs.ExprHole;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssert;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtAssume;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.typs.StructDef;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeStructRef;
import sketch.compiler.ast.promela.stmts.StmtFork;
import sketch.compiler.ast.spmd.stmts.StmtSpmdfork;
import sketch.compiler.dataflow.DataflowWithFixpoint;
import sketch.compiler.dataflow.MethodState.Level;
import sketch.compiler.dataflow.abstractValue;
import sketch.compiler.dataflow.nodesToSB.IntVtype;
import sketch.compiler.dataflow.recursionCtrl.RecursionControl;
import sketch.compiler.passes.lowering.EliminateReturns;
import sketch.compiler.passes.lowering.SymbolTableVisitor;
import sketch.compiler.spin.IdentifyModifiedVars;
import sketch.compiler.stencilSK.VarReplacer;
import sketch.util.exceptions.ExceptionAtNode;
import sketch.util.exceptions.SketchNotResolvedException;

/**
 *
 * The sketch preprocessor mainly does constant propagation and inlining of functions and unrolling of loops.
 * After this step, all the holes are now regarded as static holes.
 * @author asolar
 *
 */
public class PreprocessSketch extends DataflowWithFixpoint {

    public Map<String, Function> newFuns;

    private boolean inlineStatics = false;



    public Object visitExprStar(ExprHole star) {
        Object obj = super.visitExprStar(star);
        if (!inlineStatics && !star.isGlobal) {
            ExprHole old = (ExprHole)exprRV;
            ExprHole n = new ExprHole(old);
            n.extendName(rcontrol.callStack());
            if (old.special())
                n.makeSpecial(old.parentHoles());
            exprRV = n;
        }
        return obj;
    }

    public Object visitExprNew(ExprNew exp) {
        Object obj = super.visitExprNew(exp);
        ExprNew nexp = (ExprNew) this.exprRV;
        if (nexp.isHole()) {
            ExprHole star = nexp.getStar();
            star.accept(this);
            ExprHole newStar = (ExprHole) exprRV;
            if (newStar != star) {
                exprRV = new ExprNew(nexp, nexp.getTypeToConstruct(), nexp.getParams(), 
                        true, newStar);
            } else {
                exprRV = nexp;
            }
        } else {
            TypeStructRef sr = (TypeStructRef) exp.getTypeToConstruct();
            StructDef sd = nres.getStruct(sr.getName());
            if (!sd.getPkg().equals(currentTopPkg)) {
                List<Type> tpm = nexp.getTypeParams();
                nexp = new ExprNew(nexp, new TypeStructRef(sd.getFullName(), sr.isUnboxed(), tpm), nexp.getParams(), 
                        nexp.isHole());
                exprRV = nexp;
            }

        }
        return obj;
    }

    @Override
    public String transName(String name){
        return state.transName(name);
    }


    public PreprocessSketch(TempVarGen vargen, int maxUnroll, RecursionControl rcontrol, boolean uncheckedArrays, boolean inlineStatics){
        super(new IntVtype(), vargen,true, maxUnroll, rcontrol );
        newFuns = new HashMap<String, Function>();
        this.uncheckedArrays = uncheckedArrays;
        this.inlineStatics = inlineStatics;
    }

    public PreprocessSketch(TempVarGen vargen, int maxUnroll, RecursionControl rcontrol, boolean uncheckedArrays){
        super(new IntVtype(), vargen,true, maxUnroll, rcontrol );
        newFuns = new HashMap<String, Function>();
        this.uncheckedArrays = uncheckedArrays;
    }

    public PreprocessSketch(TempVarGen vargen, int maxUnroll, RecursionControl rcontrol){
        super(new IntVtype(), vargen,true, maxUnroll, rcontrol );
        newFuns = new HashMap<String, Function>();
    }



     protected void startFork(StmtFork loop){
         IdentifyModifiedVars imv = new IdentifyModifiedVars();
            loop.getBody().accept(imv);
            state.pushParallelSection(imv.lhsVars);
        }

     protected void startFork(StmtSpmdfork stmt){
         IdentifyModifiedVars imv = new IdentifyModifiedVars();
            stmt.getBody().accept(imv);
            state.pushParallelSection(imv.lhsVars);
     }

    boolean hasAssumes = false;

    public Object visitStmtAssume(StmtAssume stmt) {
        hasAssumes = true;
        return super.visitStmtAssume(stmt);
    }

    @Override
     public Object visitStmtAssert (StmtAssert stmt) {
            /* Evaluate given assertion expression. */
            Expression assertCond = stmt.getCond();
            abstractValue vcond  = (abstractValue) assertCond.accept (this);
        if (vcond.hasIntVal() && vcond.getIntVal() == 0) {
            abstractValue vcrv = state.getRvflag().state(vtype);
            if (this.checkTA() ||
                    (vcrv.hasIntVal() && vcrv.getIntVal() == 0 && !hasAssumes))
            {
                throw new ArrayIndexOutOfBoundsException(
                        "ASSERTION CAN NOT BE SATISFIED: " +
                    stmt.getCx() + " " + stmt.getMsg());
            }
        }
            Expression ncond = exprRV;
            String msg = null;
            msg = stmt.getMsg();
            state.Assert(vcond, stmt);
            return isReplacer ?(
(vcond.hasIntVal() && vcond.getIntVal() == 1) ? null
                : new StmtAssert(stmt, ncond, stmt.getMsg(), stmt.isSuper(),
                        stmt.getAssertMax(), stmt.isHard)
                    )
                    : stmt
                    ;
        }

    int funcount = 0;

    String freshFunName(String base) {
        String newName = base + funcount;
        ++funcount;
        while (nres.getFun(newName) != null) {
            newName = base + funcount;
            ++funcount;
        }
        return newName;
    }
    Object specializeUninterpGen(Function fun, ExprFunCall exp) {
        String newName = freshFunName(exp.getName());
        Function newFun = fun.creator().name(newName).create();
        funcsToAnalyze.add(newFun);
        nres.registerFun(newFun);
        return super.visitExprFunCall(new ExprFunCall(exp, newName, exp.getParams(), exp.getTypeParams()));
    }

    public Object processEqualsCall(Function fun, ExprFunCall exp) {
        Iterator<Expression> actualParams = exp.getParams().iterator();
        if (exp.getParams().size() < 3) {
            throw new ExceptionAtNode(exp, "This cannot be an @IsEquals function.");
        }
        abstractValue avleft = null;
        abstractValue avright = null;
        lhsVisitor lhsv = null;
        Expression nlhs = null;
        Iterator<Parameter> formalParams = fun.getParams().iterator();

        while (actualParams.hasNext()) {
            Expression actual = actualParams.next();
            Parameter param = formalParams.next();
            if (param.isParameterOutput()) {

                lhsv = new lhsVisitor();
                nlhs = (Expression) actual.accept(lhsv);

            }
            if (param.isParameterInput()) {
                abstractValue av = (abstractValue) actual.accept(this);
                if (avleft == null) {
                    avleft = av;
                } else {
                    if (avright == null)
                        avright = av;
                }
            }
        }
        abstractValue outval = vtype.eq(avleft, avright);

        {
            String lhsName = null;
            abstractValue lhsIdx = null;
            int rlen = -1;

            lhsName = lhsv.lhsName;
            lhsIdx = lhsv.lhsIdx;
            rlen = lhsv.rlen;
            boolean isFieldAcc = lhsv.isFieldAcc;

            if (!isFieldAcc) {
                assignmentToLocal(outval, lhsName, lhsIdx, rlen);
            } else {
                boolean tmpir = isReplacer;
                isReplacer = false;
                assignmentToField(lhsName, null, lhsIdx, outval, null, null);
                isReplacer = tmpir;
            }
        }

        if (isReplacer) {
            if (outval.hasIntVal()) {
                addStatement(new StmtAssign(nlhs, new ExprConstInt(outval.getIntVal())));
                exprRV = null;
            } else {
                Object obj = super.visitExprFunCall(exp);
                return obj;
            }
        } else {
            exprRV = exp;
        }

        // assert !isReplacer : "A replacer should really do something different
        // with function calls.";
        return outval;
    }

    public Object visitExprFunCall(ExprFunCall exp)
    {
        String name = exp.getName();
        Function fun = nres.getFun(name);
        String funPkg = fun.getPkg();
        if (!funPkg.equals(currentTopPkg) && !name.contains("@")) {
            exp = new ExprFunCall(exp, fun.getFullName(), exp.getParams(), exp.getTypeParams());
        }

        if(fun.getSpecification()!= null){
            String specName = fun.getSpecification();
            if (newFuns.containsKey(nres.getFunName(specName))) {
                fun = newFuns.get(nres.getFunName(specName));
            }else{
                Function newFun = nres.getFun(specName);
                assert newFun.isStatic();
                fun = newFun;
                /*
                 * Level lvl = state.pushLevel("ExprFunCall(" + exp.getName() + ")"); try
                 * { fun = (Function)this.visitFunction(newFun); } catch (Throwable e) {
                 * printFailure("error in visitExprFunCall for ", exp); }
                 * state.popLevel(lvl); newFuns.put(nres.getFunName(specName), fun);
                 */
            }
        }
        if (fun != null) {
            if( fun.isUninterp()  || ( fun.isStatic() && !inlineStatics   ) ){
                if(fun.isStatic()){
                    funcsToAnalyze.add(fun);
                }

                if (fun.hasAnnotation("IsEquals")) {
                    return processEqualsCall(fun, exp);
                }

                if (fun.isUninterp() && fun.isGenerator()) {
                    // Specialize Uninterp Generator.
                    return specializeUninterpGen(fun, exp);
                }

                return super.visitExprFunCall(exp);
            }else{
                if(inlineStatics){
                    assert fun.isStatic() : " If you are in inlinestatics mode, you should only have statics or uninterpreted functions.";
                }
                FEReplacer elimr = new EliminateReturns();
                elimr.setNres(nres);
                fun = (Function) fun.accept(elimr);
                
                List<String> tps = fun.getTypeParams();
                if (!tps.isEmpty()) {
                    List<Type> lt = new ArrayList<Type>();
                    for (Expression actual : exp.getParams()) {
                        lt.add(getType(actual));
                    }
                    TypeRenamer tr = SymbolTableVisitor.getRenaming(fun, lt, nres, null);
                    fun = (Function) fun.accept(tr);
                }

                if (rcontrol.testCall(exp)) {
                    inliner2(exp, fun);
                }else{
                    StmtAssert sas = new StmtAssert(exp, ExprConstInt.zero, false);
                    addStatement(sas);
                }
                exprRV = null;
                return vtype.BOTTOM();
            }
        }
        exprRV = null;
        return vtype.BOTTOM();
    }

    private Expression transExpr(Expression e) {
        if (e instanceof ExprVar) {
            return e;
        }
        // TODO: This makes the semantics of functions vs generators slightly different.
        return e;
    }

    public void inliner2(ExprFunCall exp, Function fun) {
        /* Increment inline counter. */
        rcontrol.pushFunCall(exp, fun);

        try {
            List<Statement> oldNewStatements = newStatements;
            newStatements = new ArrayList<Statement>();
            Statement result = null;
            int level = state.getLevel();
            int ctlevel = state.getCTlevel();
            Level lvl = state.pushLevel("visitExprFunCall2 " + exp.getName());
            try {
                Level lvl2;
                List<Expression> nactuals = new ArrayList<Expression>();
                List<Parameter> nparams = new ArrayList<Parameter>();
                Statement nbody;
                {
                    Iterator<Expression> actualParams = exp.getParams().iterator();
                    Map<String, Expression> rmap = new HashMap<String, Expression>();
                    for (Parameter p : fun.getParams()) {
                        Expression act = actualParams.next();
                        if (p.isParameterOutput()) {
                            rmap.put(p.getName(), transExpr(act));
                        } else {
                            nactuals.add(act);
                            nparams.add(p);
                        }
                    }
                    VarReplacer vr = new VarReplacer(rmap);
                    nbody = (Statement) fun.getBody().accept(vr);
                }

                {

                    lvl2 =
                            inParameterSetter(exp, nparams.iterator(),
                                    nactuals.iterator(), false);
                }
                Package oldpkg = nres.curPkg();
                nres.setPackage(pkgs.get(fun.getPkg()));
                try {
                    Statement body = (Statement) nbody.accept(this);
                    addStatement(body);
                } finally {
                    Iterator<Expression> actualParams = nactuals.iterator();
                    Iterator<Parameter> formalParams = nparams.iterator();
                    outParameterSetter(formalParams, actualParams, false, lvl2);
                    nres.setPackage(oldpkg);
                }
                result = new StmtBlock(exp, newStatements);
            } finally {
                state.popLevel(lvl);
                assert level == state.getLevel() : "PreprocessSketch inliner2: Somewhere we lost a level!!";
                assert ctlevel == state.getCTlevel() : "PreprocessSketch inliner2: Somewhere we lost a ctlevel!!";
                newStatements = oldNewStatements;
            }
            addStatement(result);
        } finally {
            rcontrol.popFunCall(exp);
        }
    }

    public void inliner(ExprFunCall exp, Function fun) {
        /* Increment inline counter. */
        rcontrol.pushFunCall(exp, fun);

        try {
            List<Statement> oldNewStatements = newStatements;
            newStatements = new ArrayList<Statement>();
            Statement result = null;
            int level = state.getLevel();
            int ctlevel = state.getCTlevel();
            Level lvl = state.pushLevel("visitExprFunCall2 " + exp.getName());
            try {
                Level lvl2;
                {
                    Iterator<Expression> actualParams = exp.getParams().iterator();
                    Iterator<Parameter> formalParams = fun.getParams().iterator();
                    lvl2 = inParameterSetter(exp, formalParams, actualParams, false);
                }
                try {
                    Statement body = (Statement) fun.getBody().accept(this);
                    addStatement(body);
                } finally {
                    Iterator<Expression> actualParams = exp.getParams().iterator();
                    Iterator<Parameter> formalParams = fun.getParams().iterator();
                    outParameterSetter(formalParams, actualParams, false, lvl2);
                }
                result = new StmtBlock(exp, newStatements);
            } finally {
                state.popLevel(lvl);
                assert level == state.getLevel() : "PreprocessSketch inliner: Somewhere we lost a level!!";
                assert ctlevel == state.getCTlevel() : "PreprocessSketch inliner: Somewhere we lost a ctlevel!!";
                newStatements = oldNewStatements;
            }
            addStatement(result);
        } finally {
            rcontrol.popFunCall(exp);
        }
    }
    
    
    String currentTopPkg = null;

    public Object visitFunction(Function func){
        hasAssumes = false;
        currentTopPkg = func.getPkg();
        if (newFuns.containsKey(nres.getFunName(func.getName()))) {
            return newFuns.get(nres.getFunName(func.getName()));
        }
        Function obj;
        try {
            obj = (Function) super.visitFunction(func);
        } catch (ArrayIndexOutOfBoundsException e) {
            if (func.isSketchHarness() || func.getSpecification() != null) {
                throw new SketchNotResolvedException("", e.getMessage());
            }
            obj =
                    func.creator().body(
                            new StmtBlock(new StmtAssert(func.getCx(), ExprConstInt.zero,
                                    "This function should never be called. Will cause " +
                                            e.getMessage(), false))).create();
        }
        
        newFuns.put(nres.getFunName(obj.getName()), obj);
        return obj;
    }


}
