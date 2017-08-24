package sketch.compiler.dataflow;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.FieldDecl;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Function.FcnType;
import sketch.compiler.ast.core.NameResolver;
import sketch.compiler.ast.core.Package;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.SymbolTable;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.*;
import sketch.compiler.ast.core.exprs.ExprArrayRange.RangeLen;
import sketch.compiler.ast.core.stmts.*;
import sketch.compiler.ast.core.typs.StructDef;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.ast.core.typs.TypeStructRef;
import sketch.compiler.ast.cuda.exprs.CudaInstrumentCall;
import sketch.compiler.ast.cuda.exprs.CudaThreadIdx;
import sketch.compiler.ast.promela.stmts.StmtFork;
import sketch.compiler.ast.spmd.exprs.SpmdNProc;
import sketch.compiler.ast.spmd.exprs.SpmdPid;
import sketch.compiler.ast.spmd.stmts.StmtSpmdfork;
import sketch.compiler.dataflow.MethodState.ChangeTracker;
import sketch.compiler.dataflow.MethodState.Level;
import sketch.compiler.dataflow.deadCodeElimination.EliminateDeadCode;
import sketch.compiler.dataflow.preprocessor.PreprocessSketch;
import sketch.compiler.dataflow.recursionCtrl.RecursionControl;
import sketch.compiler.passes.lowering.SymbolTableVisitor;
import sketch.compiler.stencilSK.VarReplacer;
import sketch.util.datastructures.TypedHashMap;
import sketch.util.exceptions.ExceptionAtNode;
import sketch.util.exceptions.SketchException;

public class PartialEvaluator extends SymbolTableVisitor {
    protected MethodState state;
    protected RecursionControl rcontrol;
    /* Bounds for loop unrolling and function inlining (initialized arbitrarily). */
    protected int MAX_UNROLL = 0;
    private TempVarGen varGen;
    protected abstractValueType vtype;
    protected Expression exprRV=null;
    protected boolean isReplacer;
    protected boolean uncheckedArrays = true;
    public boolean isPrecise = true;
    protected int throwAssertionsFalse = 0;
    protected int throwAssertionsTrue = 0;

    protected void pushTrue() {
        throwAssertionsTrue++;
    }

    protected void popTA() {
        if (throwAssertionsTrue > 0) {
            throwAssertionsTrue--;
        } else {
            throwAssertionsFalse--;
        }
    }

    protected boolean checkTA() {
        return throwAssertionsTrue > 0;
    }

    protected void makeTrueFalse() {
        throwAssertionsFalse += throwAssertionsTrue;
        throwAssertionsTrue = 0;
    }

    protected List<Function> funcsToAnalyze = null;
    private Set<String> funcsAnalyzed = null;


    public String transName(String name){
        return name;
    }



    public PartialEvaluator(abstractValueType vtype, TempVarGen varGen,
            boolean isReplacer, int maxUnroll, RecursionControl rcontrol)
    {
        super(null);
        this.MAX_UNROLL = maxUnroll;
        this.rcontrol = rcontrol;
        this.varGen = varGen;
        this.vtype = vtype;
        vtype.setPeval(this);

        this.state = new MethodState(vtype);
        this.isReplacer =  isReplacer;
    }

    protected boolean intToBool(int v) {
        if(v>0)
            return true;
        else
            return false;
    }

    protected int boolToInt(boolean b) {
        if(b)
            return 1;
        else
            return 0;
    }

    protected void report(boolean t, String s) {
        if(!t){
            System.err.println(s);
            System.err.println(nres.curPkg().getCx());
            throw new RuntimeException(s);
        }
    }

    public Object visitExprTuple(ExprTuple exp) {
        List<Expression> elems = exp.getElements();
        List<abstractValue> newElementValues = new ArrayList<abstractValue>(elems.size());
        List<Expression> newElements = new ArrayList<Expression>(elems.size());

        for (Expression elt : elems) {
            abstractValue newElement = (abstractValue) elt.accept(this);
            newElementValues.add(newElement);
            if (isReplacer) {
                newElements.add(exprRV);
            }
        }

        if (isReplacer) {
            exprRV = new ExprTuple(exp, newElements, exp.getName());
        }
        return vtype.TUPLE(newElementValues, exp.getName());
    }

    public Object visitExprArrayInit(ExprArrayInit exp) {
        List<Expression> elems = exp.getElements();
        List<abstractValue> newElementValues = new ArrayList<abstractValue>(elems.size());
        List<Expression> newElements = new ArrayList<Expression>(elems.size());

        for (Expression elt : elems) {
            abstractValue newElement = (abstractValue) elt.accept(this);
            newElementValues.add(newElement);
            if (isReplacer) {
                newElements.add(exprRV);
            }
        }

        if (isReplacer) {
            exprRV = new ExprArrayInit(exp, newElements);
        }
        return vtype.ARR(newElementValues);
    }

    public Object visitExprTupleAccess(ExprTupleAccess exp) {

        int index = exp.getIndex();

        Expression nstart = exprRV;
        abstractValue newBase = (abstractValue) exp.getBase().accept(this);
        Expression nbase = exprRV;
        if (isReplacer) {
            exprRV = new ExprTupleAccess(exp, nbase, index);
        }

        try {
            return vtype.tupleacc(newBase, vtype.CONST(index));
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new ArrayIndexOutOfBoundsException(exp.getCx() + ":" + e.getMessage() +
                    ":" + exp);
        }
    }
    public Object visitExprArrayRange(ExprArrayRange exp) {        
        RangeLen rl = exp.getSelection();
        abstractValue newStart = (abstractValue) rl.start().accept(this);
        Expression nstart = exprRV;
        abstractValue newBase = (abstractValue) exp.getBase().accept(this);
        Expression nbase = exprRV;

        abstractValue newLen = null;
        Expression nlen = null;
        if(rl.hasLen()){
            newLen = (abstractValue) rl.getLenExpression().accept(this);
            nlen = exprRV;
        }

        if(isReplacer ){

            if(nbase instanceof ExprArrayInit && newStart.hasIntVal()){
                if(!rl.hasLen()){
                    ExprArrayInit eai = (ExprArrayInit) nbase;
                    try {
                        exprRV = eai.getElements().get(newStart.getIntVal());
                    } catch (IndexOutOfBoundsException ex) {
                        throw new ArrayIndexOutOfBoundsException(ex.getMessage());
                    }
                }else{
                    Integer ln = rl.getLenExpression().getIValue();
                    if(ln != null){
                        int i= newStart.getIntVal();
                        List<Expression> elems = new ArrayList<Expression>(ln);
                        ExprArrayInit aib = (ExprArrayInit) nbase;
                        for(int t=0; t<ln; ++t ){
                            try {
                            elems.add( aib.getElements().get(i+t) );
                            } catch (IndexOutOfBoundsException ex) {
                                throw new ArrayIndexOutOfBoundsException(ex.getMessage());
                            }
                        }
                        exprRV = new ExprArrayInit(exp, elems);
                    }else{
                        exprRV = new ExprArrayRange(exp, nbase, new RangeLen(nstart, nlen), exp.isUnchecked());
                    }
                }
            }else{
                exprRV = new ExprArrayRange(exp, nbase, new RangeLen(nstart, nlen), exp.isUnchecked());
            }
        }

        try{
            return vtype.arracc(newBase, newStart, newLen, exp.isUnchecked() || uncheckedArrays);
        }catch(ArrayIndexOutOfBoundsException e){
            throw new ArrayIndexOutOfBoundsException( exp.getCx() + ":"  + e.getMessage() + ":" + exp );
        }
    }


    public Object visitExprConstFloat(ExprConstFloat exp) {
        exprRV = exp;
        return vtype.BOTTOM(exp.toString());
    }

    public Object visitExprConstInt(ExprConstInt exp) {
        exprRV = exp;
        return vtype.CONST(  exp.getVal() );
    }

    public Object visitExprNullPtr(ExprNullPtr nptr){
        exprRV = nptr;
        return vtype.NULL();
    }


    @Override
    public Object visitCudaThreadIdx(CudaThreadIdx cudaThreadIdx) {
        if (isReplacer) {
            exprRV = (Expression) super.visitCudaThreadIdx(cudaThreadIdx);
        }
        return vtype.BOTTOM(TypePrimitive.inttype);
    }


    @Override
    public Object visitSpmdPid(SpmdPid pid) {
        if (isReplacer) {
            exprRV = (Expression) super.visitSpmdPid(pid);
        }
        return vtype.BOTTOM(TypePrimitive.inttype);
    }

    @Override
    public Object visitSpmdNProc(SpmdNProc spmdnproc) {
        if (isReplacer) {
            exprRV = (Expression) super.visitSpmdNProc(spmdnproc);
        }
        return vtype.BOTTOM(TypePrimitive.inttype);
    }

    @Override
    public Object visitStmtSpmdfork(StmtSpmdfork stmt) {
        Statement body = null;
        Expression nproc = null;
        Level lvl = state.pushLevel("StmtSpmdFork");
        try {
            // TODO xzl: is this unnecessary?
            startSpmdfork(null);
            try {
                abstractValue vnproc = (abstractValue) stmt.getNProc().accept(this);
                nproc = exprRV;
                body = (Statement) stmt.getBody().accept(this);

            } finally {
                // TODO xzl: is this unnecessary?
                state.popParallelSection();
            }
        } finally {
            state.popLevel(lvl);
        }
        return isReplacer ? new StmtSpmdfork(stmt.getCx(), null, nproc, body) : stmt;
    }

    public Object visitExprField(ExprField exp) {
        abstractValue retVal = vtype.BOTTOM();
        Expression origLeft = exp.getLeft();
        abstractValue av = (abstractValue) origLeft.accept(this);
        Expression left = exprRV;
        String right = exp.getName();

        Map<String, Map<String, abstractValue>> cases =
                ((abstractValue) av).getADTcases();
        if (cases != null) {
            Type typ = null;
            try {
                typ = getType(left);
            } catch (RuntimeException e) {
                // System.out.println(e);
            }
            if (!(typ != null && typ.isStruct())) {
                try {
                    typ = getType(origLeft);
                } catch (RuntimeException e) {
                    // System.out.println(e);
                }
            }
            if (typ != null && typ.isStruct()) {
                String caseName = ((TypeStructRef) typ).getName();
                caseName = nres.getStructName(caseName);
                if (cases.containsKey(caseName)) {
                    Map<String, abstractValue> fields = cases.get(caseName);
                    if (fields.containsKey(right)) {
                        retVal = fields.get(right);
                    }
                }
            }
        }

        if (isReplacer) {
            if (retVal != null && retVal.hasIntVal()) {
                exprRV = new ExprConstInt(retVal.getIntVal());
            } else {
                exprRV = new ExprField(exp, left, right, exp.isHole());
            }
        }
        return retVal;
    }

    public Object visitExprTernary(ExprTernary exp) {
        abstractValue cond = (abstractValue) exp.getA().accept(this);
        Expression ncond = exprRV;

        boolean wereArraysUnchecked = uncheckedArrays;

        uncheckedArrays = (!cond.hasIntVal () || cond.getIntVal () == 0);
        abstractValue vtrue = (abstractValue) exp.getB().accept(this);
        Expression nvtrue = exprRV;

        uncheckedArrays = (!cond.hasIntVal () || cond.getIntVal () != 0);
        abstractValue vfalse = (abstractValue) exp.getC().accept(this);
        Expression nvfalse = exprRV;

        uncheckedArrays = wereArraysUnchecked;

        switch (exp.getOp())
        {
            case ExprTernary.TEROP_COND:
                if (isReplacer) {
                    if (cond.hasIntVal())
                        exprRV = (cond.getIntVal() == 0) ? nvfalse : nvtrue;
                    else
                        exprRV =
                                new ExprTernary(exp, exp.getOp(), ncond, nvtrue, nvfalse);
                }
                return vtype.ternary(cond, vtrue, vfalse);

            default:
                exp.assertTrue(false, "unknown ternary operator");
                return null;
        }
    }

    public Object visitExprTypeCast(ExprTypeCast exp) {
        abstractValue childExp = (abstractValue) exp.getExpr().accept(this);
        Expression narg = exprRV;
        Type t = (Type) exp.getType().accept(this);
        if(isReplacer) exprRV = new ExprTypeCast(exp, t, narg );
        return vtype.cast(childExp, t);
    }

    public Object visitExprVar(ExprVar exp) {
        String vname =  exp.getName();
        abstractValue val = state.varValue(vname);
        if (isReplacer)
            if (val.hasIntVal()) {
                exprRV = new ExprConstInt(val.getIntVal());
            } else {
                if (fields != null && fields.contains(exp.getName())) {
                    exprRV = exp;
                } else {
                    exprRV = new ExprVar(exp, transName(exp.getName()));
                }
            }
        // FIXME xzl: is this correct? shouldn't we set exprRV = exp if !isReplacer ?
        return  val;
    }

    public Object visitExprConstChar(ExprConstChar exp)
    {
        exprRV = exp;
        return vtype.CONST(exp.getId());
    }

    public Object visitExprUnary(ExprUnary exp) {

        abstractValue childExp = (abstractValue) exp.getExpr().accept(this);
        Expression nexp =   exprRV;
        if(isReplacer) exprRV = new ExprUnary(exp, exp.getOp(), nexp);
        switch(exp.getOp())
        {
            case ExprUnary.UNOP_NOT:
                return  vtype.not( childExp );
            case ExprUnary.UNOP_BNOT:
                return  vtype.not( childExp );
            case ExprUnary.UNOP_NEG:
                return  vtype.neg( childExp );
            case ExprUnary.UNOP_PREINC:
            {
                assert exp.getExpr() instanceof ExprVar : "Pre and post inc only on variables.";
                String name = ((ExprVar)exp.getExpr()).getName();
                childExp =  vtype.plus(childExp, vtype.CONST(1));
                state.setVarValue(name, childExp);
                return childExp;

            }
            case ExprUnary.UNOP_POSTINC:
            {
                assert exp.getExpr() instanceof ExprVar : "Pre and post inc only on variables.";
                String name = ((ExprVar)exp.getExpr()).getName();
                state.setVarValue(name, vtype.plus(childExp, vtype.CONST(1)));
                return childExp;

            }
            case ExprUnary.UNOP_PREDEC:
            {
                assert exp.getExpr() instanceof ExprVar : "Pre and post inc only on variables.";
                String name = ((ExprVar)exp.getExpr()).getName();
                childExp =  vtype.plus(childExp, vtype.CONST(-1));
                state.setVarValue(name, childExp);
                return childExp;

            }
            case ExprUnary.UNOP_POSTDEC:
            {
                assert exp.getExpr() instanceof ExprVar : "Pre and post inc only on variables.";
                String name = ((ExprVar)exp.getExpr()).getName();
                state.setVarValue(name, vtype.plus(childExp, vtype.CONST(-1)));
                return childExp;

            }
        }
        assert false;
        return null;
    }


    public Object visitExprBinary(ExprBinary exp)
    {

        abstractValue left = (abstractValue) exp.getLeft().accept(this);
        Expression nleft =   exprRV;

        abstractValue right = null;
        Expression nright =   null;
        int op = exp.getOp();
        if(op != ExprBinary.BINOP_BAND && op != ExprBinary.BINOP_BOR && op != ExprBinary.BINOP_AND && op != ExprBinary.BINOP_OR){
            right = (abstractValue) exp.getRight().accept(this);
            nright =   exprRV;
        }
        abstractValue rv = null;



        switch (exp.getOp())
        {
            case ExprBinary.BINOP_ADD: rv = vtype.plus(left, right); break;
            case ExprBinary.BINOP_SUB: rv = vtype.minus(left, right); break;
            case ExprBinary.BINOP_MUL: rv = vtype.times(left, right); break;
            case ExprBinary.BINOP_DIV: rv = vtype.over(left, right); break;
            case ExprBinary.BINOP_MOD: rv = vtype.mod(left, right); break;
            case ExprBinary.BINOP_EQ:  rv = vtype.eq(left, right); break;
            case ExprBinary.BINOP_NEQ: rv = vtype.not(vtype.eq(left, right)); break;
            case ExprBinary.BINOP_LT: rv = vtype.lt(left, right); break;
            case ExprBinary.BINOP_LE: rv = vtype.le(left, right); break;
            case ExprBinary.BINOP_GT: rv = vtype.gt(left, right); break;
            case ExprBinary.BINOP_GE: rv = vtype.ge(left, right); break;

            case ExprBinary.BINOP_AND:
            case ExprBinary.BINOP_BAND:{
                if(left.hasIntVal() && left.getIntVal() == 0){
                    rv = vtype.CONST(0);
                }else{
                    right = (abstractValue) exp.getRight().accept(this);
                    nright =   exprRV;
                    rv = vtype.and(left, right);
                }
                break;
            }

            case ExprBinary.BINOP_OR:
            case ExprBinary.BINOP_BOR:{
                if(left.hasIntVal() && left.getIntVal() == 1){
                    rv = vtype.CONST(1);
                }else{
                    right = (abstractValue) exp.getRight().accept(this);
                    nright =   exprRV;
                    rv = vtype.or(left, right);
                }
                break;
            }

            case ExprBinary.BINOP_BXOR: rv = vtype.xor(left, right); break;
            case ExprBinary.BINOP_SELECT:
                abstractValue choice = vtype.STAR(exp);
                rv = vtype.condjoin(choice, left, right);
                break;
            case ExprBinary.BINOP_LSHIFT:
                rv = vtype.shl(left, right);
            case ExprBinary.BINOP_RSHIFT:
                rv = vtype.shr(left, right);
        }


        if(isReplacer){
            if(rv.hasIntVal() ){
                exprRV = new ExprConstInt(rv.getIntVal());
                return rv;
            }else{
                if (exp.getOp() == ExprBinary.BINOP_ADD) {
                    if ((right.hasIntVal() && right.getIntVal() == 0) ||
                            nright.equals(ExprConstInt.zero))
                    {
                        exprRV = nleft;
                        return rv;
                    } else {
                        if ((left.hasIntVal() && left.getIntVal() == 0) ||
                                nleft.equals(ExprConstInt.zero))
                        {
                            exprRV = nright;
                            return rv;
                        } else {
                            exprRV = new ExprBinary(exp, exp.getOp(), nleft, nright);
                            return rv;
                        }
                    }
                } else {
                    if (exp.getOp() == ExprBinary.BINOP_MUL) {
                        if ((right.hasIntVal() && right.getIntVal() == 0) ||
                                nright.equals(ExprConstInt.zero))
                        {
                            exprRV = ExprConstInt.zero;
                            return rv;
                        } else {
                            if ((left.hasIntVal() && left.getIntVal() == 0) ||
                                    nleft.equals(ExprConstInt.zero))
                            {
                                exprRV = ExprConstInt.zero;
                                return rv;
                            }
                        }
                        if ((right.hasIntVal() && right.getIntVal() == 1) ||
                                nright.equals(ExprConstInt.one))
                        {
                            exprRV = nleft;
                            return rv;
                        } else {
                            if ((left.hasIntVal() && left.getIntVal() == 1) ||
                                    nleft.equals(ExprConstInt.one))
                            {
                                exprRV = nright;
                                return rv;
                            }
                        }
                    }
                    exprRV = new ExprBinary(exp, exp.getOp(), nleft, nright);
                }
            }
        }



        return rv;

    }
    public Object visitExprStar(ExprStar star) {
        Type t = (Type) star.getType().accept(this);
        star.setType(t);
        exprRV = star;
        return vtype.STAR(star);
    }

    protected Expression interpretActualParam(Expression e) {
        return e;
    }


    public Object visitExprFunCall(ExprFunCall exp)
    {
        String name = exp.getName();
        Iterator<Expression> actualParams = exp.getParams().iterator();
        List<abstractValue> avlist = new ArrayList<abstractValue>(exp.getParams().size());

        List<Expression> nparams = new ArrayList<Expression>(exp.getParams().size());
        Function fun = nres.getFun(name);
        assert fun != null : " The function " + name + " does not exist!! funcall: " + exp;
        Iterator<Parameter> formalParams = fun.getParams().iterator();
        Map<String, Expression> pmap = new HashMap<String, Expression>();
        VarReplacer vrep = new VarReplacer(pmap);
        List<Parameter> nplist = new ArrayList<Parameter>();
        List<lhsVisitor> tempLHSVs = new ArrayList<lhsVisitor>();
        // List<Expression> expsToAssign = new ArrayList<Expression>();

        while(actualParams.hasNext()){
            Expression actual = actualParams.next();
            Parameter param = formalParams.next();            
            Type paramType = (Type) ((Type)param.getType().accept(vrep)).accept(this);            
            pmap.put(param.getName(), interpretActualParam(actual));
            nplist.add(new Parameter(param, param.getSrcTupleDepth(), paramType,
                    param.getName(), param.getPtype()));
            boolean addedAlready = false;
            if (param.isParameterOutput()) {
                // if (actual instanceof ExprVar) {
                // String pnm = ((ExprVar) actual).getName();
                // outNmList.add(pnm);
                // nparams.add(new ExprVar(exp, transName(pnm)));
                // addedAlready = true;
                // } else {
                // String pnm = varGen.nextVar();
                // tempVars.add(pnm);
                // expsToAssign.add(actual);
                // outNmList.add(pnm);
                // (new StmtVarDecl(exp, paramType, pnm, actual)).accept(this);
                //
                //
                // }

                lhsVisitor lhsv = new lhsVisitor();
                Expression nlhs = (Expression) actual.accept(lhsv);
                tempLHSVs.add(lhsv);
                nparams.add(nlhs);
                addedAlready = true;

            }
            if(param.isParameterInput()){
                abstractValue av = (abstractValue)actual.accept(this);
                if(!addedAlready){
                    nparams.add(exprRV);
                }
                if(paramType instanceof TypeArray ){
                    TypeArray ta = (TypeArray) paramType;

                    abstractValue fav = null;
                    if(av.isVect()){
                        List<abstractValue> lv = av.getVectValue();
                        Expression lexp = ta.getLength();
                        if (lexp == null) {
                            fav = (av);
                        }else{
                            Integer tal = lexp.getIValue();
                            if (tal != null && lv.size() == tal) {
                                fav = (av);
                            } else {
                                fav = (vtype.cast(av, ta));
                            }
                        }
                    } else {                        
                        fav = av;
                    }
                    avlist.add(fav);
                }else{
                    assert !av.isVect() || av.getVectValue().size() == 1;
                    avlist.add(av);
                }
            }
        }
        List<abstractValue> outSlist = new ArrayList<abstractValue>();
        // Function nfun = fun.creator().params(nplist).create();
        try {
            vtype.funcall(fun, avlist, outSlist, state.pathCondition(true), state,
                    exp.getClusterId());
        } catch (SketchException se) {
            throw new ExceptionAtNode(se.getMessage(), exp);
        }


        assert outSlist.size() == tempLHSVs.size() : "The funcall in vtype should populate the outSlist with 1 element per output parameter";

        Iterator<lhsVisitor> lhsIt = tempLHSVs.iterator();
        for (abstractValue outval : outSlist) {
            // state.setVarValue(nmIt.next(), it.next());

            String lhsName = null;
            abstractValue lhsIdx = null;
            int rlen = -1;

            lhsVisitor lhsv = lhsIt.next();
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



        //assert !isReplacer : "A replacer should really do something different with function calls.";
        exprRV = isReplacer ?  new ExprFunCall(exp, name, nparams)  : exp ;
        return  vtype.BOTTOM();
    }



    @Override
    public Object visitCudaInstrumentCall(CudaInstrumentCall instrumentCall) {
        if (isReplacer) {
            instrumentCall.getToImplement().accept(this);
            ExprVar expr2 = (ExprVar) exprRV;
            instrumentCall.getImplVariable().accept(this);
            ExprVar expr3 = (ExprVar) exprRV;
            if (expr2 != instrumentCall.getToImplement() ||
                    expr3 != instrumentCall.getImplVariable())
            {
                return new CudaInstrumentCall(instrumentCall, expr2, expr3,
                        instrumentCall.getImplName());
            } else {
                exprRV = instrumentCall;
            }
            return exprRV;
        }
        return vtype.BOTTOM();
    }



    public class lhsVisitor extends FEReplacer{
        //public Expression nlhs;
        public String lhsName=null;
        public int rlen = -1;
        public abstractValue lhsIdx = null;
        public boolean isFieldAcc = false;
        private Type t;

        public abstractValue typeLen(Type t){
            if( t instanceof TypeArray){
                TypeArray ta = (TypeArray) t;
                abstractValue len;
                if (ta.getLength() != null) {
                    len = (abstractValue) ta.getLength().accept(PartialEvaluator.this);
                } else {
                    len = vtype.BOTTOM("FARRAY");
                }
                //abstractValue olen = typeLen(ta.getBase());
                return len; //vtype.times(len, olen);
            }else{
                assert  t instanceof TypePrimitive : "NYI" ;
                return vtype.CONST(1);
            }
        }


        public Object visitExprField(ExprField exp)
        {
            isFieldAcc = true;
            super.visitExprField(exp);
            PartialEvaluator.this.visitExprField(exp);
            assert t.isStruct();

            // FIXME xzl: ideally we want this to be done all the time!
            // but when it is replacer, we have to compute tlen in VisitEAR
            // and that tlen will use field names, causing problems!
            // if (false && PartialEvaluator.this instanceof EliminateDeadCode) {
            // TypeStruct ts = (TypeStruct) actualType(t);
            // t = ts.getType(exp.getName());
            // }
            return PartialEvaluator.this.exprRV;
            //return super.visitExprField(exp);
        }

        public Object visitExprTupleAccess(ExprTupleAccess exp){
            Expression base = exp.getBase();
            Expression nbase = (Expression) base.accept(this);
            int index = exp.getIndex();
            if (t instanceof TypeStructRef) {
                TypeStructRef ta = (TypeStructRef) t;
                StructDef str = nres.getStruct(ta.getName());
                int size = str.getNumFields();
                if (index < 0 || index >= size) {
                    throw new ArrayIndexOutOfBoundsException(exp.getCx() +
                            " ARRAY OUT OF BOUNDS !(0<=" + index + " < " + size + ")");
                }
            }

            if (isReplacer) {
                return new ExprTupleAccess(exp, nbase, index);
            } else {
                return exp;
            }

        }
        public Object visitExprArrayRange(ExprArrayRange ear){
            Expression base = ear.getBase();
            Expression nbase = (Expression) base.accept(this);
            RangeLen rl = ear.getSelection();
            // _debug.append(" ear: " + ear);
            // _debug.append(" base:" + base + " nbase:" + nbase + " ");
            abstractValue olidx=null;
            olidx = lhsIdx;
            lhsIdx = (abstractValue)rl.start().accept(PartialEvaluator.this);
            abstractValue llhsIdx = lhsIdx;
            Expression idxExpr = exprRV;

            // FIXME xzl:
            // why do we need to do the checking here? it's an EAR, does t have to be
            // TypeArray?
            if( t instanceof TypeArray ){
                TypeArray ta = (TypeArray) t;
                // abstractValue tlen = isReplacer ? typeLen(ta) : null;
                abstractValue tlen = typeLen(ta);
                // _debug.append(" ta:" + ta + " tlen:" + tlen + " ");
                t = ta.getBase();
                if(olidx != null){
                    // FIXME: this is rather hacking, what we really want is
                    // joining the two Idx
                    // lhsIdx =
                    // isReplacer ? vtype.plus(lhsIdx, vtype.times(olidx, tlen))
                    // : vtype.plus(lhsIdx, olidx);
                    lhsIdx = vtype.plus(lhsIdx, vtype.times(olidx, tlen));
                }
                if (llhsIdx.hasIntVal()) {
                    int iidx = llhsIdx.getIntVal();
                    if (tlen.hasIntVal()) {
                        int size = tlen.getIntVal();
                        if(!ear.isUnchecked()&& (iidx < 0 || iidx >= size)  )
                            throw new ArrayIndexOutOfBoundsException(ear.getCx() +
                                    " ARRAY OUT OF BOUNDS !(0<=" + iidx + " < " + size +
                                    ")" /* + _debug.toString() */);
                    }
                }
            } else {
                if (olidx != null && PartialEvaluator.this instanceof EliminateDeadCode) {
                    lhsIdx = vtype.plus(lhsIdx, olidx);
                }
            }

            if(rl.hasLen()){
                abstractValue lenav = (abstractValue) rl.getLenExpression().accept(PartialEvaluator.this);
                if(lenav.hasIntVal()){
                    rlen = lenav.getIntVal();
                }else{
                    rlen = -2;
                }
                if(isReplacer){                                        
                    return  new ExprArrayRange(ear, nbase, new RangeLen(idxExpr, PartialEvaluator.this.exprRV), ear.isUnchecked());                    
                }else{
                    return ear;
                } 
            }else{
                rlen = 1;
                if(isReplacer){
                    return  new ExprArrayRange(ear, nbase, idxExpr, ear.isUnchecked());
                }else{
                    return ear;
                }
            }
        }
        public Object visitExprVar(ExprVar expr){
            assert lhsName == null;
            lhsName = expr.getName();
            t = state.varType(lhsName);
            if(isReplacer) return new ExprVar(expr, transName(lhsName));
            else return expr;
        }
    }



    public Object visitStmtAssign(StmtAssign stmt)
    {

        boolean isFieldAcc;
        abstractValue rhs = null;
        try{
            rhs = (abstractValue) stmt.getRHS().accept(this);
        }catch(ArrayIndexOutOfBoundsException e){
            String msg = e.getMessage() + ":" + stmt;
            throw new ArrayIndexOutOfBoundsException(msg);
        }
        Expression nrhs = exprRV;

        Expression lhs = stmt.getLHS();

        String lhsName = null;
        abstractValue lhsIdx = null;
        Expression nlhs = null;
        int rlen = -1;

        lhsVisitor lhsv = new lhsVisitor();
        nlhs = (Expression)lhs.accept(lhsv);
        lhsName = lhsv.lhsName;
        lhsIdx = lhsv.lhsIdx;
        rlen = lhsv.rlen;
        isFieldAcc = lhsv.isFieldAcc;



        switch(stmt.getOp())
        {
            case ExprBinary.BINOP_ADD:
                assert rlen <= 1 : "Operand not supported for this operator: " + stmt;
                rhs = vtype.plus((abstractValue) lhs.accept(this), rhs);
                break;
            case ExprBinary.BINOP_SUB:
                assert rlen <= 1 : "Operand not supported for this operator: " + stmt;
                rhs = vtype.minus((abstractValue) lhs.accept(this), rhs);
                break;
            case ExprBinary.BINOP_MUL:
                assert rlen <= 1 : "Operand not supported for this operator: " + stmt;
                rhs = vtype.times((abstractValue) lhs.accept(this), rhs);
                break;
            case ExprBinary.BINOP_DIV:
                assert rlen <= 1 : "Operand not supported for this operator: " + stmt;
                rhs = vtype.over((abstractValue) lhs.accept(this), rhs);
                break;
            case 0:
                break;
            default:
                throw new ExceptionAtNode("Operator not supported", stmt);
        }
        if (!isFieldAcc) {
            assignmentToLocal(rhs, lhsName, lhsIdx, rlen);
        } else {
            return assignmentToField(lhsName, stmt, lhsIdx, rhs, nlhs, nrhs);
        }
        return isReplacer?  new StmtAssign(stmt, nlhs, nrhs, stmt.getOp())  : stmt;
    }

    protected void assignmentToLocal(abstractValue rhs, String lhsName,
            abstractValue lhsIdx, int rlen) {
        if(rlen == -2){
            state.setVarValue(lhsName, vtype.BOTTOM(), rhs);
        } else if (rlen <= 1) {
            state.setVarValue(lhsName, lhsIdx, rhs);
        }else{
            List<abstractValue> lst = null;
            if(rhs.isVect()){
                lst = rhs.getVectValue();
            }
            for(int i=0; i<rlen; ++i){
                if(i==0){
                    if(lst != null){
                        if(lst.size() > i ){
                            state.setVarValue(lhsName, lhsIdx, lst.get(i));
                        }else{
                            state.setVarValue(lhsName, lhsIdx, vtype.CONST(0));
                        }
                    }else{
                        state.setVarValue(lhsName, lhsIdx, rhs);
                    }
                }else{
                    if(lst != null && lst.size() > i){
                        state.setVarValue(lhsName, vtype.plus(lhsIdx, vtype.CONST(i) ), lst.get(i));
                    }else{
                        if (rhs.isBottom()) {
                            state.setVarValue(lhsName,
                                    vtype.plus(lhsIdx, vtype.CONST(i)), vtype.BOTTOM());
                        } else {
                            state.setVarValue(lhsName,
                                    vtype.plus(lhsIdx, vtype.CONST(i)), vtype.CONST(0));
                        }
                    }
                }
            }
        }
    }


    protected Object assignmentToField(String lhsName, StmtAssign stmt,
            abstractValue idx, abstractValue rhs, Expression nlhs, Expression nrhs)
    {
        return isReplacer?  new StmtAssign(stmt, nlhs, nrhs, stmt.getOp())  : stmt;
    }



    public Object visitStmtBlock(StmtBlock stmt)
    {

        // Put context label at the start of the block, too.
        Statement s = null;
        int level = state.getLevel();
        // Stack<String> prevLS = state.getLevelStack();
        int ctlevel = state.getCTlevel();
        Level lvl = null;
        Exception e = null;
        try{
            lvl = state.pushLevel(new BlockLevel("PartialEvaluator level"));
            s = (Statement)super.visitStmtBlock(stmt);
        }
        // catch (Exception e1) {e = e1; }
        finally{
            if( s == null){
                s = stmt;
            }
            state.popLevel(lvl);
            // if (level != state.getLevel()) {
            // printFailure("Somewhere we lost a level!!");
            // printFailure("Expected", prevLS);
            // printFailure("Actual", state.getLevelStack());
            // printFailure("Before popping", beforePop);
            // printFailure("Approximate source location", stmt.getCx());
            // printFailure("Block", stmt);
            // printFailure("related exception", e);
            // assertFalse();
            // }
            assert level == state.getLevel() : "PartialEvaluator visitStmtBlock: Somewhere we lost a level!!";
            assert ctlevel == state.getCTlevel() : "PartialEvaluator visitStmtBlock: Somewhere we lost a ctlevel!!";
        }
        return s;
    }

    protected static class BlockLevel extends Level {
        public BlockLevel(String msg) {
            super(msg);
        }

        @Override
        public String toString() {
            return "BlockLevel[" + msg + ", isDead=" + isDead + "]";
        }
    }

    public Object visitStmtReorderBlock(StmtReorderBlock block){
        throw new UnsupportedOperationException();
    }


    @Override
    public Object visitParameter(Parameter param){
        Type ntype = (Type)param.getType().accept(this);
        state.varDeclare(param.getName() , ntype);
        symtab.registerVar(transName(param.getName()), ntype, param, SymbolTable.KIND_FUNC_PARAM);
        if(isReplacer){            
            return new Parameter(param, param.getSrcTupleDepth(), ntype,
                    transName(param.getName()),
                    param.getPtype());
        }else{
            return param;
        }
    }

    public Object visitFunction(Function func)
    {

        Level lvl = state.beginFunction(func.getName());

        List<Parameter> params = func.getParams();
        List<Parameter> nparams = isReplacer ? new ArrayList<Parameter>() : null;
        for(Iterator<Parameter> it = params.iterator(); it.hasNext(); ){
            Parameter param = it.next();
            Parameter p  = (Parameter) param.accept(this);
            if(isReplacer){
                nparams.add(p);
            }           
        }

        Statement newBody;
        try {

            newBody = (Statement) func.getBody().accept(this);

        } catch (ArrayIndexOutOfBoundsException e) {
            newBody =
                    new StmtBlock(new StmtAssert(func.getBody(), ExprConstInt.zero,
                            "This function should never be called. Will cause " +
                                    e.getMessage(), false));
        }

        state.endFunction(lvl);

        return isReplacer ? func.creator().params(nparams).body(newBody).create() : null;

        //state.pushVStack(new valueClass((String)null) );
    }


    public Object visitStmtBreak(StmtBreak stmt)
    {
        stmt.assertTrue (false, "Sorry, 'break' statements are not yet evaluated");
        return "break";
    }

    public Object visitStmtContinue(StmtContinue stmt)
    {
        stmt.assertTrue (false, "Sorry, 'continue' statements are not yet evaluated");
        return "continue";
    }


    public Object visitStmtExpr(StmtExpr stmt)
    {
        Expression exp = stmt.getExpression();
        exp.accept(this);
        Expression nexp = exprRV;
        return isReplacer? ( nexp == null ? null : new StmtExpr(stmt, nexp) )  :stmt;
    }


    /**
     *
     * This method must necessarily push a parallel section.
     * This implementation is the most conservative implementation for this methods.
     * In some cases, we can be more liberal and pass a subset of the variables
     * that we want to make volatile in the fork. For things like constant propagation,
     * we only need to make volatile those variables that are modified in the fork.
     *
     * EliminateTransAssign in particular requires the conservative version of the method.
     *
     */
    protected void startFork(StmtFork loop){
        state.pushParallelSection();
    }

    protected void startSpmdfork(StmtSpmdfork stmt) {
        // TODO xzl: is this unnecessary?
        state.pushParallelSection();
    }

    public Object visitStmtFork(StmtFork loop){

        startFork(loop);

        Statement nbody = null;
        StmtVarDecl ndecl = null;
        Expression niter = null;
        try{
            Level lvl = state.pushLevel("StmtFork");
            abstractValue viter = (abstractValue) loop.getIter().accept(this);
            niter = exprRV;
            try{
                ndecl = (StmtVarDecl) loop.getLoopVarDecl().accept(this);
                nbody = (Statement)loop.getBody().accept(this);
            }finally{
                state.popLevel(lvl);
            }
        }finally{
            state.popParallelSection();
        }
        return isReplacer?  new StmtFork(loop, ndecl, niter, nbody) : loop;
    }

    public Object visitStmtFor(StmtFor stmt)
    {
        Level lvl = state.pushLevel("StmtFor");
        int unrollamnt = this.MAX_UNROLL;
        try{
            if (stmt.getInit() != null)
                stmt.getInit().accept(this);
            report( stmt.getCond() != null , "For now, the condition in your for loop can't be null");
            abstractValue vcond = (abstractValue) stmt.getCond().accept(this);
            int iters = 0;

            while (!vcond.isBottom() &&
 vcond.getIntVal() > 0 &&
                    ((!state.rvflag.absVal.isBottom() && state.rvflag.absVal.getIntVal() == 0) || stmt.isCanonical()))
            {
                ++iters;
                stmt.getBody().accept(this);
                if (stmt.getIncr() != null){
                    stmt.getIncr().accept(this);
                }
                vcond = (abstractValue) stmt.getCond().accept(this);
                if (iters > (1 << 13)) {
                    // printWarning("Loop seems to repeat more than 2^13 times",
                    // stmt.getCx());
                    throw new ArrayIndexOutOfBoundsException(stmt.getCx() +
                            "Loop seems to repeat more than 2^13 times");
                }
            }
            if (!vcond.isBottom() && vcond.getIntVal() > 0) {
                Expression ubound = stmt.getRangeMax();
                Expression lbound = stmt.getRangeMin();
                Expression incr = stmt.getIncrVal();
                if (ubound != null && lbound != null && incr != null) {
                    Integer ub = ubound.getIValue();
                    Integer lb = lbound.getIValue();
                    Integer i = incr.getIValue();
                    if (ub != null && lb != null && i != null) {
                        int tmp = (ub.intValue() - lb.intValue() + 1) / i.intValue();
                        if (tmp > 0 && tmp < this.MAX_UNROLL * 10) {
                            unrollamnt = tmp;
                        }
                    }
                }
            }


            if (vcond.isBottom() || vcond.getIntVal() > 0) {
                int remIters = unrollamnt - iters;
                if(remIters > 0){
                    if (stmt.isCanonical()) {
                        Statement nbody =
                                new StmtIfThen(stmt, stmt.getCond(), stmt.getBody(), null);
                        for (int i = 0; i < remIters; ++i) {
                            nbody.accept(this);
                            if (stmt.getIncr() != null) {
                                stmt.getIncr().accept(this);
                            }
                        }
                        Statement cur =
                                new StmtAssert(
                                        stmt,
                                        new ExprUnary("!", stmt.getCond()),
                                        stmt.getCx() +
                                                ": This loop was unrolled " +
                                                MAX_UNROLL +
                                                " times, but apparently that was not enough. Use the --bnd-unroll-amnt flag for better results.",
                                        false);
                        cur.accept(this);
                    } else {
                        Statement body = stmt.getBody();
                        if (stmt.getIncr() != null) {
                            body = new StmtBlock(body, stmt.getIncr());
                        }

                        Statement cur =
                                new StmtAssert(
                                        stmt,
                                        new ExprUnary("!", stmt.getCond()),
                                        stmt.getCx() +
                                                ": This loop was unrolled " +
                                                MAX_UNROLL +
                                                " times, but apparently that was not enough. Use the --bnd-unroll-amnt flag for better results.",
                                        false);

                        for (int i = 0; i < remIters; ++i) {
                            cur =
                                    new StmtIfThen(stmt, stmt.getCond(), new StmtBlock(
                                            body, cur), null);
                        }

                        cur.accept(this);
                    }
                    /*
                     * String doneNm = this.varGen.nextVar("done"); ExprVar doneVar = new
                     * ExprVar(stmt, doneNm); StmtVarDecl svd = new StmtVarDecl(stmt,
                     * TypePrimitive.bittype, doneNm, ExprConstInt.zero); Expression cond
                     * = new ExprBinary(stmt.getCond(), "&&", new ExprUnary("!",
                     * doneVar)); Statement setDone = new StmtAssign(doneVar,
                     * ExprConstInt.one); Statement body = stmt.getBody(); if
                     * (stmt.getIncr() != null){ body = new StmtBlock(body,
                     * stmt.getIncr()); } Statement condIter = new StmtIfThen(stmt, cond,
                     * body, setDone); svd.accept(this); for(int i=0; i<remIters; ++i){
                     * condIter.accept(this); } StmtAssert as = new StmtAssert(stmt,new
                     * ExprBinary(new ExprUnary("!", stmt.getCond()), "||", doneVar) ,
                     * "This loop was unrolled " + MAX_UNROLL
                     * +" times, but apparently that was not enough."); as.accept(this);
                     */
                }else{
                    StmtAssert as = new StmtAssert(stmt, new ExprUnary("!", stmt.getCond()), "This loop was unrolled " + MAX_UNROLL +" times, but apparently that was not enough.", false);
                    as.accept(this);
                }
            }
        }
        // catch(Exception e) { printWarning("suppressing", e); }
        finally{
            state.popLevel(lvl);
        }
        assert !isReplacer : "No replacement policy for this yet.";
        return stmt;
    }

    public Object visitStmtSwitch(StmtSwitch stmt) {
        List<String> cases = stmt.getCaseConditions();
        int nCases = cases.size();
        assert nCases > 0 : "StmtSwitch must have a branch";
        // NOTE xzl: currently we do not replace StmtSwitch with only one case to the sole
        // body, because SymbolTableVisitor relies on StmtSwitch to infer the refined type
        // of the matched variable inside each Case

        ExprVar var = stmt.getExpr();
        abstractValue vcond = (abstractValue) var.accept(this);
        ExprVar ncond = isReplacer ? (ExprVar) exprRV : var;
        Map<String, Map<String, abstractValue>> knownCases = vcond.getADTcases();
        int uncaughtCases = Integer.MAX_VALUE;
        if (knownCases != null) {
            uncaughtCases = knownCases.size();
        }

        List<ChangeTracker> changeTrackers = new ArrayList<ChangeTracker>(nCases);

        StmtSwitch newStmt = new StmtSwitch(stmt.getContext(), ncond);
        String pkg = null;
        ExprVar realCond = ncond;
        // This is a big hack. It's here to make up for the fact that the
        // subclasses are not consistently delcaring vars.
        // TODO: Fix.
        for (int i = 0; i < 5; ++i) {
            try {
                pkg = nres.getStruct(((TypeStructRef) getType(realCond)).getName()).getPkg();
                if (pkg != null) {
                    break;
                }
            } catch (RuntimeException e) {
                // ignore
            }
            realCond = new ExprVar(ncond, transName(realCond.getName()));
        }
        for (String caseExpr : cases) {
            TypeStructRef ntype = null;
            if ("default".equals(caseExpr)) {
                // FIXME xzl: is this the correct behavior? "default" will catch all
                // cases even if there's still concrete cases below it.
                uncaughtCases = 0;
            } else {
                ntype = (new TypeStructRef(caseExpr, false));
                if (pkg != null) {
                    ntype = ntype.addDefaultPkg(pkg, nres);
                }
                if (knownCases != null) {
                    if (knownCases.containsKey(ntype.getName())) {
                        // caught a concreate case
                        --uncaughtCases;
                    } else {
                        // we know about vcond, and caseExpr is not a possible case
                        continue;
                    }
                }
            }

            SymbolTable oldSymTab1 = symtab;
            symtab = new SymbolTable(symtab);

            if (caseExpr != "default") {
                symtab.registerVar(ncond.getName(), new TypeStructRef(caseExpr, false), stmt, SymbolTable.KIND_LOCAL);
            }
            Statement body = null;
            String error = null;
            ChangeTracker thisChange = null;
            // NOTE xzl: we should push a new level of ChangeTracker so that the state change in this Case remains local in this Case
            // and do not affect other Cases below it. Then we pop it out to collect the state change of this Case.
            // In the end we should merge all ChangeTrackers to form the joint state change of this StmtSwitch.
            state.pushChangeTracker(vcond, false);
            try {
                if (!"default".equals(caseExpr)) {
                    symtab = new SymbolTable(symtab);
                    symtab.registerVar(ncond.getName(), ntype);
                }

                body = (Statement) stmt.getBody(caseExpr).accept(this);
                /*
                 * if (isReplacer) { if (body != null) { Statement ts = (new
                 * CloneHoles()).process(body); body = (Statement) ts.accept(this); } }
                 */

            } catch (ArrayIndexOutOfBoundsException e) {
                // IF the body throws this exception, it means that no matter what the
                // input,
                // if this branch runs, it will cause the exception, so we can just assert
                // that this
                // branch will never run.
                error = e.getMessage();
            } catch (ArithmeticException e) {
                error = e.getMessage();
            } catch (RuntimeException e) {
                // Something we cannot handle
                throw e;
            } finally {
                symtab = oldSymTab1;
                thisChange = state.popChangeTracker();
            }
            if (error != null) {
                body = new StmtAssert(stmt, ExprConstInt.zero, error, false);
            } else {
                if (body == null) {
                    // NOTE xzl: even if body == null or empty, we still must collect the
                    // ChangeTracker, because later the changeTrackers is processed
                    // destructively, and an NOP branch effectively prevents anything to
                    // be updated destructively.
                    body = new StmtBlock(new ArrayList<Statement>());
                }
                // NOTE xzl: a body without any error, we should collect the ChangeTracker
                changeTrackers.add(thisChange);
            }
            newStmt.addCaseBlock(caseExpr, body);

            if (uncaughtCases <= 0) {
                // NOTE xzl: this depends on that all cases in StmtSwitch must be
                // exclusive. no other cases to match, just stop.
                break;
            }
        }
        state.procChangeTrackers(changeTrackers);
        return isReplacer ? newStmt : stmt;

    }

    public void checkCondEquality(Expression cond) {
        if ((this instanceof PreprocessSketch)) {
            if (cond instanceof ExprBinary) {
                ExprBinary eb = (ExprBinary) cond;
                if (eb.getOp() == ExprBinary.BINOP_EQ) {
                    if (eb.getLeft() instanceof ExprVar) {
                        if (eb.getRight() instanceof ExprConstInt) {
                            state.setVarValue(eb.getLeft().toString(), (abstractValue) eb.getRight().accept(this));
                        }
                    }
                }
                if (eb.getOp() == ExprBinary.BINOP_AND) {
                    checkCondEquality(eb.getLeft());
                    checkCondEquality(eb.getRight());
                }
            }
        }
    }

    public Object visitStmtIfThen(StmtIfThen stmt)
    {
        // must have an if part...

        Expression cond = stmt.getCond();
        abstractValue vcond = (abstractValue)cond.accept(this);
        Expression ncond = isReplacer ? exprRV : cond;

        Statement cons = stmt.getCons();
        if(cons != null && !(cons instanceof StmtBlock)){
            cons = new StmtBlock(cons);
        }

        Statement alt = stmt.getAlt();
        if(alt != null && !(alt instanceof StmtBlock)){
            alt = new StmtBlock(alt);
        }


        if(vcond.hasIntVal()){
            if(vcond.getIntVal() != 0){ // vtrue
                Statement rv ;
                if( rcontrol.testBlock(cons) ){
                    try {
                        rv = (Statement) cons.accept(this);
                    } finally {
                        rcontrol.doneWithBlock(cons);
                    }
                }else{
                    StmtAssert sa = new StmtAssert(stmt, ExprConstInt.zero, false);
                    sa.setMsg( rcontrol.debugMsg() );
                    rv = (Statement)( sa ).accept(this);
                }
                return rv;
            }else{
                if (alt != null){
                    Statement rv ;
                    if( rcontrol.testBlock(alt) ){
                        try {
                            rv = (Statement) alt.accept(this);
                        } finally {
                            rcontrol.doneWithBlock(alt);
                        }
                    }else{
                        StmtAssert sa = new StmtAssert(stmt, ExprConstInt.zero, false);
                        sa.setMsg( rcontrol.debugMsg() );
                        rv =(Statement)( sa ).accept(this);
                    }
                    return rv;
                }
            }
            return null;
        }

        /* Attach conditional to change tracker. */
        int ctlevel = state.getCTlevel();
        state.pushChangeTracker (vcond, false);
        Statement nvtrue = null;
        Statement nvfalse = null;
        boolean oldTA = false;
        if( rcontrol.testBlock(cons) ){
            try{
                try {
                    pushTrue();
                    checkCondEquality(cond);
                    nvtrue = (Statement) cons.accept(this);
                } finally {
                    popTA();
                }
            }catch(ArrayIndexOutOfBoundsException e){
                //IF the body throws this exception, it means that no matter what the input,
                //if this branch runs, it will cause the exception, so we can just assert that this
                //branch will never run.
                //In order to improve the precision of the analysis, we pop the dirty change tracker,
                //and push in a clean one, so the rest of the function thinks that nothing at all was written in this branch.
                state.popChangeTracker();
                addStatement((Statement) (new StmtAssert(stmt, new ExprUnary("!", ncond),
                        e.getMessage(),
                        false)).accept(this));
                nvtrue = null;
                ncond = ExprConstInt.zero;
                state.pushChangeTracker(vcond, false);
            }catch(ArithmeticException e){
                state.popChangeTracker();
                addStatement((Statement) (new StmtAssert(stmt, new ExprUnary("!", ncond),
                        false)).accept(this));
                nvtrue = null;
                ncond = ExprConstInt.zero;
                state.pushChangeTracker (vcond, false);
            } catch (RuntimeException e) {
                state.popChangeTracker();
                rcontrol.doneWithBlock(cons);
                throw e;
                //throw e;
            }
            rcontrol.doneWithBlock(cons);
        } else {
            state.popChangeTracker();
            StmtAssert sa = new StmtAssert(stmt, new ExprUnary("!", ncond), false);
            sa.setMsg( rcontrol.debugMsg() );
            addStatement((Statement) (sa).accept(this));
            nvtrue = null;
            ncond = ExprConstInt.zero;
            state.pushChangeTracker(vcond, false);
        }
        ChangeTracker ipms = state.popChangeTracker();
        assert state.getCTlevel() == ctlevel : "Somewhere we lost a ctlevel!! " + ctlevel + " != " + state.getCTlevel();

        ChangeTracker epms = null;
        if (alt != null){
            /* Attach inverse conditional to change tracker. */
            state.pushChangeTracker (vcond, true);
            if( rcontrol.testBlock(alt) ){

                try{
                    try {
                        pushTrue();
                        nvfalse = (Statement) alt.accept(this);
                    } finally {
                        popTA();
                    }
                }catch(ArrayIndexOutOfBoundsException e){
                    state.popChangeTracker();
                    addStatement((Statement) (new StmtAssert(stmt, ncond, false)).accept(this));
                    ncond = ExprConstInt.one;
                    nvfalse = null;
                    state.pushChangeTracker(vcond, true);
                }catch(Throwable e){
                    state.popChangeTracker();
                    rcontrol.doneWithBlock(alt);
                    throw new RuntimeException(e);
                    //throw e;
                }
                rcontrol.doneWithBlock(alt);
            }else{
                state.popChangeTracker();
                StmtAssert sa = new StmtAssert(stmt, ncond, false);
                sa.setMsg( rcontrol.debugMsg() );
                addStatement((Statement) (sa).accept(this));
                ncond = ExprConstInt.one;
                nvfalse = null;
                state.pushChangeTracker(vcond, true);
            }
            epms = state.popChangeTracker();
        }
        if(epms != null){
            state.procChangeTrackers(ipms, epms);
        }else{
            state.procChangeTrackers(ipms);
        }        
        if(isReplacer){
            if(nvtrue == null && nvfalse == null){
                return null;
            }
            if(nvtrue == null){
                nvtrue = StmtEmpty.EMPTY;
            }
            return new StmtIfThen(stmt,ncond, nvtrue, nvfalse );
        }else{
            return stmt;
        }
    }

    /**
     * Assert statement visitor. Generates a complex assertion expression which
     * takes into consideration the chain of nesting conditional expressions, and
     * composes them as premises to the given expression.
     *
     * @author Gilad Arnold
     */
    public Object visitStmtAssert (StmtAssert stmt) {
        /* Evaluate given assertion expression. */
        Expression assertCond = stmt.getCond();
        abstractValue vcond  = (abstractValue) assertCond.accept (this);
        Expression ncond = exprRV;
        String msg = null;
        msg = stmt.getMsg();
        try{
            state.Assert(vcond, stmt);
        }catch(RuntimeException e){
            System.err.println(stmt.getCx() + ":" +  e.getMessage() );
            throw e;
        }
        return isReplacer ? new StmtAssert(stmt, ncond, stmt.getMsg(), stmt.isSuper(),
                stmt.getAssertMax(), stmt.isHard) : stmt;
    }

    public Object visitStmtAssume(StmtAssume stmt) {
        /* Evaluate given assertion expression. */
        Expression cond = stmt.getCond();
        abstractValue vcond = (abstractValue) cond.accept(this);
        Expression ncond = exprRV;
        String msg = null;
        msg = stmt.getMsg();
        try {
            state.Assume(vcond, stmt);
        } catch (RuntimeException e) {
            System.err.println(stmt.getCx() + ":" + e.getMessage());
            throw e;
        }
        return isReplacer ? new StmtAssume(stmt, ncond, stmt.getMsg()) : stmt;
    }

    @Override
    public Object visitStmtMinimize(StmtMinimize stmtMinimize) {
        stmtMinimize.getMinimizeExpr().accept(this);
        return isReplacer ? new StmtMinimize(exprRV, stmtMinimize.userGenerated) : stmtMinimize;
    }

    public Object visitStmtLoop(StmtLoop stmt)
    {
        /* Generate a new variable, initialized with loop expression. */

        List<Statement> slist = isReplacer? new ArrayList<Statement>() : null;

        FENode nvarContext = stmt;
        String nvar = varGen.nextVar ();
        StmtVarDecl nvarDecl =
                new StmtVarDecl(nvarContext, TypePrimitive.inttype, nvar, stmt.getIter());
        Statement tmpstmt = (Statement) nvarDecl.accept (this);
        if(isReplacer) slist.add(tmpstmt);

        /* Generate and visit an expression consisting of the new variable. */
        ExprVar nvarExp =
 new ExprVar(nvarContext, nvar);
        abstractValue vcond  = (abstractValue)nvarExp.accept (this);

        /* If no known value, perform conditional unrolling of the loop. */
        if (!vcond.hasIntVal()) {
            /* Assert loop expression does not exceed max unrolling constant. */
            StmtAssert nvarAssert =
                    new StmtAssert(nvarContext, new ExprBinary(
nvarContext,
                            ExprBinary.BINOP_LE, new ExprVar(nvarContext, nvar),
                            new ExprConstInt(nvarContext, MAX_UNROLL)), StmtAssert.UBER);
            tmpstmt = (Statement)nvarAssert.accept (this);
            if(isReplacer) slist.add(tmpstmt);
            List<Expression> condlist = isReplacer ? new ArrayList<Expression>() : null;
            List<Statement> bodlist = isReplacer ? new ArrayList<Statement>() : null;
            int iters;
            for (iters=0; iters < MAX_UNROLL; ++iters) {
                /* Generate context condition to go with change tracker. */
                Expression guard =
                        new ExprBinary(nvarContext, ExprBinary.BINOP_GT, new ExprVar(
                                nvarContext, nvar), new ExprConstInt(nvarContext, iters));
                abstractValue vguard = (abstractValue) guard.accept (this);
                Expression nguard = isReplacer ? exprRV : guard;

                assert (vguard.isBottom());
                state.pushChangeTracker (vguard, false);
                Statement nbody = null;
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try{

                    nbody = (Statement)(new CloneHoles()).process(stmt.getBody()).accept(this);
                }catch(ArrayIndexOutOfBoundsException er){
                    //If this happens, it means that we statically determined that unrolling by (iters+1) leads to an out
                    //of bounds error. Thus, we will put a new assertion on the loop condition.
                    state.popChangeTracker();
                    StmtAssert nvarAssert2 =
                            new StmtAssert(nvarContext, new ExprUnary("!", nguard), false);
                    nbody = (Statement) nvarAssert2.accept (this);

                    if(isReplacer){
                        condlist.add(nguard);
                        bodlist.add(nbody);
                    }
                    break;
                }catch(Throwable e){
                    for(int i=iters; i>=0; --i){
                        ChangeTracker ipms = state.popChangeTracker();
                    }
                    throw new RuntimeException(e);
                    //throw e;
                }
                if(isReplacer){
                    condlist.add(nguard);
                    bodlist.add(nbody);
                }

            }

            assert (isReplacer? iters == condlist.size() || iters+1 == condlist.size() : true) : "This is wierd";

            StmtIfThen ifthen = null;

            if(isReplacer){
                if( iters+1 == condlist.size() ){
                    int i=iters;
                    ifthen = new StmtIfThen(stmt, condlist.get(i), bodlist.get(i), null);
                }
            }

            for(int i=iters-1; i>=0; --i){
                ChangeTracker ipms = state.popChangeTracker();
                state.procChangeTrackers(ipms);

                if(isReplacer){
                    FENode cx = stmt;
                    if(ifthen == null){
                        ifthen = new StmtIfThen(cx, condlist.get(i), bodlist.get(i), null);
                    }else{
                        List<Statement> nlist = new ArrayList<Statement>(2);
                        nlist.add(bodlist.get(i));
                        nlist.add(ifthen);
                        ifthen = new StmtIfThen(cx, condlist.get(i), new StmtBlock(cx, nlist), null);
                    }
                }
            }
            if(isReplacer){
                slist.add(ifthen);
            }
            return isReplacer? new StmtBlock(stmt, slist) : stmt;
        }else{
            List<Statement> tlist = isReplacer? new ArrayList<Statement>( vcond.getIntVal() ) : null;
            for(int i=0; i<vcond.getIntVal(); ++i){
                Statement itstmt = (Statement)(new CloneHoles()).process(stmt.getBody()).accept(this);
                if(isReplacer) tlist.add( itstmt );
            }
            return isReplacer? new StmtBlock(stmt, tlist) : stmt;
        }
    }

    public Object visitStmtReturn(StmtReturn stmt)
    {
        makeTrueFalse();
        // state.testReturn();
        state.freturn();
        return super.visitStmtReturn(stmt);
    }

    public Object visitTypeArray(TypeArray t) {
        Type nbase = (Type)t.getBase().accept(this);
        abstractValue avlen = (abstractValue) t.getLength().accept(this);
        Expression nlen = exprRV;
        if(nbase == t.getBase() &&  t.getLength() == nlen ) return t;
        if (isReplacer) {
            TypeArray newtype =
                    new TypeArray(t.getCudaMemType(), nbase, nlen, t.getMaxlength());
            return newtype;
        } else {
            return t;
        }
    }

    public Set<String> fields = null;
    public Object visitStructDef(StructDef ts) {
        boolean changed = false;
        TypedHashMap<String, Type> map = new TypedHashMap<String, Type>();
        Level lvl = null;
        try {
            lvl = state.pushLevel(new BlockLevel("PartialEvaluator level"));
            fields = new HashSet<String>();

            StructDef sdf = ts;
            while (sdf != null) {
                for (Entry<String, Type> entry : sdf) {
                    fields.add(entry.getKey());
                    if (!(entry.getValue() instanceof TypeArray)) {
                        state.varDeclare(entry.getKey(), entry.getValue());
                        state.setVarValue(entry.getKey(), vtype.BOTTOM());
                    }
                }
                String pn = sdf.getParentName();
                if (pn != null) {
                    sdf = nres.getStruct(pn);
                } else {
                    sdf = null;
                }
            }
            sdf = ts;
            while (sdf != null) {
                for (Entry<String, Type> entry : sdf) {
                    if ((entry.getValue() instanceof TypeArray)) {
                        state.varDeclare(entry.getKey(), entry.getValue());
                        state.setVarValue(entry.getKey(), vtype.BOTTOM());
                    }
                }
                String pn = sdf.getParentName();
                if (pn != null) {
                    sdf = nres.getStruct(pn);
                } else {
                    sdf = null;
                }
            }

            for (Entry<String, Type> entry : ts) {
                Type type = (Type) entry.getValue().accept(this);
                changed |= (type != entry.getValue());
                map.put(entry.getKey(), type);
            }
            if (changed) {
                return ts.creator().fields(map).create();
            } else {
                return ts;
            }
        } finally {
            fields = null;
            state.popLevel(lvl);
        }
    }

    public Object visitStmtVarDecl(StmtVarDecl stmt)
    {
        List<Type> types = isReplacer? new ArrayList<Type>() : null;
        List<String> names = isReplacer? new ArrayList<String>() : null;
        List<Expression> inits = isReplacer? new ArrayList<Expression>() : null;
        for (int i = 0; i < stmt.getNumVars(); i++)
        {
            String nm = stmt.getName(i);
            Type vt = (Type)stmt.getType(i).accept(this);
            if (vt instanceof TypeStructRef) {
                StructDef sdef = null;
                String pkg = null;
                sdef = nres.getStruct(((TypeStructRef) vt).getName());
                if (sdef != null) {
                    pkg = sdef.getPkg();
                    vt = vt.addDefaultPkg(pkg, nres);
                }
            }
            state.varDeclare(nm, vt);
            String tname = transName(stmt.getName(i));
            symtab.registerVar(tname, vt, stmt, SymbolTable.KIND_LOCAL);

            Expression ninit = null;
            if( stmt.getInit(i) != null ){
                abstractValue init = (abstractValue) stmt.getInit(i).accept(this);
                ninit = exprRV;
                state.setVarValue(nm, init);
            }else{
                if(!(stmt.getType(i) instanceof TypeArray)){
                    if (stmt.getType(i) instanceof TypeStructRef) {
                        state.setVarValue(nm, this.vtype.NULL());
                    }else{
                        Expression dv = vt.defaultValue();
                        state.setVarValue(nm, (abstractValue) dv.accept(this));
                    }
                }else{                    
                    TypeArray tar = (TypeArray) vt;
                    Expression el = tar.getLength();
                    Integer iv = el != null ? el.getIValue() : null;
                    if(iv != null ){                        
                        int n = iv;
                        List<abstractValue> vals = new ArrayList<abstractValue>();
                        abstractValue dv = (abstractValue)tar.getBase().defaultValue().accept(this);
                        for(int i1 = 0; i1<n; ++i1){  vals.add(dv);  }
                        state.setVarValue(nm, this.vtype.ARR(vals));
                    } else {
                        abstractValue dv =
                                (abstractValue) tar.getBase().defaultValue().accept(this);
                        state.setVarValue(nm, dv);
                    }
                }
            }

            /* else{
                state.setVarValue(nm, this.vtype.BOTTOM("UNINITIALIZED"));
            } */
            if( isReplacer ){
                types.add(vt);
                names.add(transName(nm));
                inits.add(ninit);
            }
        }
        return isReplacer? new StmtVarDecl(stmt, types, names, inits) : stmt;
    }

    public Object visitExprADTHole(ExprADTHole exp) {
        exprRV = (Expression) super.visitExprADTHole(exp);
        return vtype.BOTTOM();
    }
    public Object visitExprNew(ExprNew expNew){
        exprRV = (Expression) super.visitExprNew(expNew);
        if (exprRV instanceof ExprNew) {
            ExprNew e = (ExprNew) exprRV;
            if (!e.isHole()) {
                TypeStructRef tref = (TypeStructRef) e.getTypeToConstruct();
                StructDef sdef = null;
                try {
                    String pkg = null;
                    sdef = nres.getStruct(tref.getName());
                    pkg = sdef.getPkg();
                    tref = tref.addDefaultPkg(pkg, nres);
                } catch (RuntimeException err) {
                    // ignore
                }
                String tname = tref.getName();
                String pname = sdef.getParentName();
                if (pname != null) {
                    Map<String, abstractValue> fields =
                            new HashMap<String, abstractValue>();
                    for (ExprNamedParam p : expNew.getParams()) {
                        String name = p.getName();
                        abstractValue value = (abstractValue) p.getExpr().accept(this);
                        fields.put(name, value);
                    }
                    Map<String, Map<String, abstractValue>> cases =
                            new HashMap<String, Map<String, abstractValue>>(1);
                    cases.put(tname, fields);
                    // NOTE xzl: must recover exprRV! We have called this visitor above!
                    exprRV = e;
                    return vtype.ADTnode(cases);
                }
            }
        }
        return vtype.BOTTOM();
    }

    protected Expression doExpression(Expression expr) {
        if (expr != null) {
            expr.accept(this);
            return (Expression) exprRV;
        } else
            return null;
    }

    public Object visitStmtWhile(StmtWhile stmt)
    {
        StmtFor sf = new StmtFor(stmt, null, stmt.getCond(), null, stmt.getBody(), false);
        Object t =  sf.accept(this);
        if(t instanceof StmtFor){
            sf = (StmtFor) t;
            return isReplacer ? new StmtWhile(stmt, sf.getCond(), sf.getBody()) : stmt;
        }else{
            return isReplacer ? t : stmt;
        }
    }


    public Object visitStmtDoWhile(StmtDoWhile stmt)
    {
        String vn = varGen.nextVar();
        ExprVar ev = new ExprVar(stmt, vn);
        Statement s = new StmtVarDecl(stmt, TypePrimitive.bittype, vn, ExprConstInt.one  );
        StmtAssign sa = new StmtAssign( ev, stmt.getCond() );
        Statement tmp = new StmtWhile(stmt, ev, new StmtBlock( stmt.getBody(), sa));
        tmp =  new StmtBlock(s, tmp).doStatement(this);
        return isReplacer? tmp : stmt;
    }




    public Object visitFieldDecl(FieldDecl field)
    {
        List<Type> types = isReplacer? new ArrayList<Type>() : null;
        List<String> names = isReplacer? new ArrayList<String>() : null;
        List<Expression> inits = isReplacer? new ArrayList<Expression>() : null;
        for (int i = 0; i < field.getNumFields(); ++i)
        {
            String lhs = field.getName(i);
            state.varDeclare(lhs, field.getType(i));
            Expression nexpr = null;
            if (field.getInit(i) != null){
                (new StmtAssign(
                        new ExprVar(field, lhs),
                        field.getInit(i))).accept(this);
                nexpr = exprRV; //This may be a bit risky, but will work for now.
            }else{
                nexpr = null;
            }
            if(isReplacer){
                types.add(field.getType(i));
                names.add(transName(lhs));
                inits.add( nexpr );
            }
        }
        return isReplacer? new FieldDecl(field, types, names, inits) :field;
    }



    protected List<Function> functionsToAnalyze(Package spec){
        SelectFunctionsToAnalyze funSelector = new SelectFunctionsToAnalyze();
        return funSelector.selectFunctions(spec, nres);
    }


    public Object visitStmtAtomicBlock (StmtAtomicBlock ab) {
        Expression exp = ab.getCond();
        if(ab.isCond()){
            abstractValue av =(abstractValue) exp.accept(this);
            if(isReplacer){
                exp = exprRV;
            }
        }
        Statement tmp = ab.getBlock().doStatement(this);
        if(!isReplacer) return ab;
        if (tmp == null){
            if(ab.isCond()){
                return new StmtAtomicBlock (ab,Collections.EMPTY_LIST, exp);
            }else{
                return null;
            }
        }else if (tmp != ab.getBlock() || exp != ab.getCond()){
            if(tmp instanceof StmtBlock){
                StmtBlock sb = (StmtBlock) tmp;
                return new StmtAtomicBlock (sb, sb.getStmts (), exp);
            }else{
                return new StmtAtomicBlock(ab, Collections.singletonList(tmp), exp);
            }
        }else{
            return ab;
        }
    }


    protected Map<String, Package> pkgs = new HashMap<String, Package>();

    public Object visitProgram(Program p) {
        // List<StreamSpec> nstr = new ArrayList<StreamSpec>();
        nres = new NameResolver(p);
        vtype.setNres(nres);
        rcontrol.setNameRes(nres);
        funcsToAnalyze = new ArrayList<Function>();
        Map<String, List<Function>> newfuns = new HashMap<String, List<Function>>();
        for (Package pkg : p.getPackages()) {
            nres.setPackage(pkg);
            List<Function> toAnalyze = functionsToAnalyze(pkg);
            funcsToAnalyze.addAll(toAnalyze);
            if (pkgs.containsKey(pkg.getName())) {
                throw new RuntimeException("Duplicate package name.");
            }
            String pkgName = pkg.getName();
            pkgs.put(pkgName, pkg);
            newfuns.put(pkgName, new ArrayList<Function>());
            // Add functions in special assert to funcsToAnalyze
            for (StmtSpAssert sa : pkg.getSpAsserts()) {
                ExprFunCall f1 = sa.getFirstFun();
                funcsToAnalyze.add(nres.getFun(nres.getFunName(f1.getName())));
                for (Expression param : f1.getParams()) {
                    if (param instanceof ExprFunCall) {
                        ExprFunCall pa = (ExprFunCall) param;
                        funcsToAnalyze.add(nres.getFun(nres.getFunName(pa.getName())));
                    }
                }

                ExprFunCall f2 = sa.getSecondFun();
                funcsToAnalyze.add(nres.getFun(nres.getFunName(f2.getName())));
                for (Expression param : f2.getParams()) {
                    if (param instanceof ExprFunCall) {
                        ExprFunCall pa = (ExprFunCall) param;
                        funcsToAnalyze.add(nres.getFun(nres.getFunName(pa.getName())));
                    }
                }

            }
        }
        if (funcsToAnalyze.size() == 0) {
            System.out.println("WARNING: Your input file contains no sketches. Make sure all your sketches use the implements keyword properly.");
        }

        funcsAnalyzed = new HashSet<String>();
        if (funcsToAnalyze.size() == 0) {
            System.out.println("WARNING: Your input file contains no sketches. Make sure all your sketches use the implements keyword properly.");
        }
        while (funcsToAnalyze.size() > 0) {
            Function f = funcsToAnalyze.get(0);
            String pkgName = f.getPkg();
            nres.setPackage(pkgs.get(pkgName));
            if (!funcsAnalyzed.contains(f.getFullName())) {

                if (!f.isUninterp()) {
                    Function nstmt = (Function) f.accept(this);
                    if (isReplacer) {
                        newfuns.get(pkgName).add(nstmt);
                    }
                } else {
                    if (f.isGenerator()) {
                        newfuns.get(pkgName).add(f.creator().type(FcnType.Uninterp).create());
                    }
                }
                funcsAnalyzed.add(f.getFullName());
            }
            Function tf = funcsToAnalyze.remove(0);
            assert tf == f;
        }

        List<Package> newPkgs = new ArrayList<Package>();
        for (Package pkg : p.getPackages()) {
            Package newPkg = preprocPkg(pkg);
            String pkgName = pkg.getName();
            newfuns.get(pkgName).addAll(newPkg.getFuncs());
            newPkgs.add(new Package(newPkg, pkgName, newPkg.getStructs(),
                    newPkg.getVars(), newfuns.get(pkgName), newPkg.getSpAsserts()));
        }

        return p.creator().streams(newPkgs).create();
    }


    public Package preprocPkg(Package spec)
 {
        Level lvl = state.pushLevel("visitStreamSpec");

        // At this point we get to ignore wholesale the stream type, except
        // that we want to save it.
        nres.setPackage(spec);
        // Output field definitions:

        List<FieldDecl> newVars = isReplacer ? new ArrayList<FieldDecl>() : null;
        List<Function> newFuncs = isReplacer ? new ArrayList<Function>() : null;

        List<StructDef> newStructs = new ArrayList<StructDef>();
        for (StructDef tsOrig : spec.getStructs()) {
            StructDef ts = (StructDef) tsOrig.accept(this);
            newStructs.add(ts);
        }

        for (Iterator iter = spec.getVars().iterator(); iter.hasNext(); )
        {
            FieldDecl varDecl = (FieldDecl)iter.next();
            FieldDecl nstmt = (FieldDecl)varDecl.accept(this);
            if( isReplacer ){ newVars.add(nstmt); }
        }

        for(Function sf : spec.getFuncs()){
            if (sf.isUninterp() && !sf.isGenerator()) {
                if( isReplacer ){ newFuncs.add(sf); }
            }
        }



        state.popLevel(lvl);


        //assert preFil.size() == 0 : "This should never happen";

        return isReplacer ? new Package(spec, spec.getName(), newStructs,
 newVars,
                newFuncs, spec.getSpAsserts()) : spec;
    }

    /**
     * This function evaluates each of the actual parameters to the functions, and collects both the transformed
     * parameters and their abstract values in the two respective arrays.
     *
     * @param formalParamIterator
     * @param actualParamIterator
     * @param actualsList OUTPUT parameter, returns the list of actual parameters.
     * @param actualsValList OUTPUT parameter, returns the list of abstractValues for the input parameters.
     */
    void evalInParam(Iterator<Parameter> formalParamIterator, Iterator<Expression> actualParamIterator, List<Expression> actualsList, List<abstractValue> actualsValList){
        while(actualParamIterator.hasNext()){
            Expression actualParam = actualParamIterator.next();
            abstractValue actualParamValue = (abstractValue) actualParam.accept(this);
            //System.out.println("act=" + actualParam + ", " + actualParamValue);
            actualParam = exprRV;
            actualsList.add(actualParam);
            actualsValList.add(actualParamValue);
        }
    }



    public Level inParameterSetter(FENode cx,  Iterator<Parameter> formalParamIterator, Iterator<Expression> actualParamIterator, boolean checkError){
        List<Expression> actualsList = new ArrayList<Expression>();
        List<abstractValue> actualsValList = new ArrayList<abstractValue>();

        evalInParam(formalParamIterator, actualParamIterator, actualsList, actualsValList);

        Level lvl = state.pushLevel("inParameter");

        Iterator<Expression> actualIterator = actualsList.iterator();
        Iterator<abstractValue> actualValIterator = actualsValList.iterator();

        while(actualIterator.hasNext()){
            Expression actualParam = actualIterator.next();
            Parameter formalParam = (Parameter) formalParamIterator.next();

            abstractValue actualParamValue = actualValIterator.next();

            String formalParamName = formalParam.getName();

            Type type = (Type) formalParam.getType().accept(this);
            if (type.isStruct()) {
                try {
                    String pkg = nres.getStruct(((TypeStructRef)type).getName()).getPkg();
                    type = type.addDefaultPkg(pkg, nres);
                } catch (RuntimeException e) {
                    // ignore
                }
            }

            StmtVarDecl varDecl = null;
            switch(formalParam.getPtype()){
                case Parameter.REF:{
                    state.outVarDeclare(formalParamName, type);
                    state.setVarValue(formalParamName, actualParamValue);
                    varDecl =
                            new StmtVarDecl(cx, type, transName(formalParam.getName()),
                                    actualParam);
                    break;
                }

                case Parameter.IN:{
                    state.varDeclare(formalParamName, type);
                    state.setVarValue(formalParamName, actualParamValue);
                    varDecl =
                            new StmtVarDecl(cx, type, transName(formalParam.getName()),
                                    actualParam);
                    break;
                }
                case Parameter.OUT:{
                    state.outVarDeclare(formalParamName, type);
                    Expression initVal = type.defaultValue();
                    varDecl =
                            new StmtVarDecl(cx, type, transName(formalParam.getName()),
                                    initVal);
                    break;
                }
            }
            // NOTE xzl: should register the new variable
            symtab.registerVar(varDecl.getName(0), varDecl.getType(0), varDecl, SymbolTable.KIND_LOCAL);
            addStatement(varDecl);
        }

        return lvl;
    }


    public void outParameterSetter(Iterator formalParamIterator, Iterator actualParamIterator, boolean checkError, Level lvl){
        FENode context = null;
        List<abstractValue> formalList = new ArrayList<abstractValue>();
        List<String> formalTransNames = new ArrayList<String>();
        while(formalParamIterator.hasNext()){
            Parameter formalParam = (Parameter) formalParamIterator.next();
            if( formalParam.isParameterOutput() ){
                String formalParamName = formalParam.getName();
                formalTransNames.add(transName(formalParamName));
                abstractValue av = state.varValue(formalParamName);
                formalList.add(av);
            }else{
                formalList.add(null);
                formalTransNames.add(null);
            }
        }

        state.popLevel(lvl);

        Iterator<abstractValue> vcIt = formalList.iterator();
        Iterator<String> fTransNamesIt = formalTransNames.iterator();

        while(actualParamIterator.hasNext()){
            Expression actualParam = (Expression)actualParamIterator.next();
            abstractValue formal = vcIt.next();
            String fTransName = fTransNamesIt.next();
            if( formal != null ){


                String lhsName = null;
                abstractValue lhsIdx = null;
                Expression nlhs = null;


                lhsVisitor lhsv = new lhsVisitor();
                nlhs = (ExprVar)actualParam.accept(lhsv);
                lhsName = lhsv.lhsName;
                lhsIdx = lhsv.lhsIdx;
                assert lhsv.rlen == -1 : "Violates invariant";


                state.setVarValue(lhsName, lhsIdx, formal);
                addStatement(new StmtAssign(nlhs, new ExprVar(context, fTransName) ));
            }
        }
    }

}
