package sketch.compiler.passes.preprocessing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.NameResolver;
import sketch.compiler.ast.core.Package;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.*;
import sketch.compiler.ast.core.stmts.*;
import sketch.compiler.ast.core.typs.StructDef;
import sketch.compiler.ast.core.typs.StructDef.StructFieldEnt;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.ast.core.typs.TypeStructRef;
import sketch.compiler.passes.lowering.SymbolTableVisitor;
import sketch.util.exceptions.ExceptionAtNode;
import sketch.util.exceptions.UnrecognizedVariableException;

class VarReplacer extends FEReplacer {
    Map<String, String> varMap;

    public VarReplacer(Map<String, String> varMap) {
        this.varMap = varMap;
    }

    @Override
    public Object visitExprVar(ExprVar exp) {
        String name = exp.getName();
        if (varMap.containsKey(name)) {
            return new ExprVar(exp, varMap.get(name));
        }
        return exp;
    }
}

/**
 * Generate harness directly from high level specification. Also extracts the hypothesis
 * for the inductive decomposition optimization. For cases where the optimization is
 * applicable, this class creates separate harnesses for each high level variant of the
 * first input (if it is an ADT).
 */
public class CreateHarnesses extends SymbolTableVisitor {
    Map<String, String> produceFuns = new HashMap<String, String>();
    Map<String, String> checkFuns = new HashMap<String, String>();
    TempVarGen vargen;
    boolean optimize;
    boolean separateHarness = false;
    int arrSize;
    int srcTupleDepth;

    public CreateHarnesses(TempVarGen varGen, boolean optimize, int arrSize,
            int srcTupleDepth)
    {
        super(null);
        this.vargen = varGen;
        this.optimize = optimize;
        this.arrSize = arrSize;
        this.srcTupleDepth = srcTupleDepth;
    }

    class ExtractInputs extends FEReplacer {
        List<String> inputParams;
        List<Type> inputTypes;
        List<Statement> body;
        Map<String, Expression> varBindings;

        public ExtractInputs(List<String> inputParams, List<Type> inputTypes,
                List<Statement> body,
                Map<String, Expression> varBindings, NameResolver nres)
        {
            this.inputParams = inputParams;
            this.inputTypes = inputTypes;
            this.body = body;
            this.varBindings = varBindings;
            this.nres = nres;
        }

        public Object visitExprFunCall(ExprFunCall exp) {
            Function fun;
            try {
                fun = nres.getFun(exp.getName(), exp);
            } catch (UnrecognizedVariableException e) {
                throw new ExceptionAtNode(exp, "Unknown function " + exp.getName());
            }

            List<Expression> fparams = exp.getParams();
            List<Parameter> fActParams = fun.getParams();
            if (fparams.size() != fActParams.size())
                throw new ExceptionAtNode(exp, "Wrong number of parameters " + exp.getName());
            List<Expression> newParams = new ArrayList<Expression>();

            if (!fparams.isEmpty()) {
                for (int i = 0; i < fActParams.size(); i++) {
                    Parameter p = fActParams.get(i);
                    Expression param = fparams.get(i).doExpr(this);
                    if (p.isParameterInput()) {
                        if (param instanceof ExprVar) {
                            String var = ((ExprVar) param).getName();
                            if (!varBindings.containsKey(var) &&
                                    !inputParams.contains(var))
                            {
                                inputParams.add(var);
                                inputTypes.add(p.getType());
                            }
                        }
                    }
                    if (p.isParameterReference()) {
                        if (param instanceof ExprVar) {
                            String old_name = ((ExprVar) param).getName();
                            String new_name = vargen.nextVar(old_name);
                            StmtVarDecl varD =
                                    new StmtVarDecl(exp, p.getType(), new_name, param);
                            body.add(varD);
                            newParams.add(new ExprVar(exp, new_name));
                        } else {
                            newParams.add(param);
                        }
                    } else {
                        newParams.add(param);
                    }
                }
            }
            return new ExprFunCall(exp, exp.getName(), newParams);
        }
    }

    @Override
    public Object visitPackage(Package pkg) {
        nres.setPackage(pkg);

        List<Function> newFuns = pkg.getFuncs();
        List<StmtSpAssert> newSpAsserts = new ArrayList<StmtSpAssert>();
        for (StmtSpAssert sa : pkg.getSpAsserts()) {
            List<String> inputParams = new ArrayList<String>();
            List<Type> inputTypes = new ArrayList<Type>();
            List<Statement> body = new ArrayList<Statement>();

            Map<String, Expression> varBindings = sa.getVarBindings();

            for (String var : sa.bindingsInOrder()) {
                Expression exp = varBindings.get(var);
                if (exp instanceof ExprFunCall) {
                    ExtractInputs ei =
                            new ExtractInputs(inputParams, inputTypes, body, varBindings,
                                    nres);
                    exp = exp.doExpr(ei);
                    varBindings.put(var, exp);
                    Type varType = getType(exp);
                    StmtVarDecl stmt = new StmtVarDecl(sa, varType, var, exp);
                    body.add(stmt);
                } else if (exp instanceof ExprConstInt) {
                    StmtVarDecl stmt =
                            new StmtVarDecl(sa, TypePrimitive.inttype, var, exp);
                    body.add(stmt);
                } else if (exp instanceof ExprNew) {
                    Type t = ((ExprNew) exp).getTypeToConstruct();
                    assert (t instanceof TypeStructRef);
                    String pname =
                            nres.getStructParentName(((TypeStructRef) t).getName());
                    if (pname != null) {
                        t = new TypeStructRef(pname, false);
                    }
                    StmtVarDecl stmt = new StmtVarDecl(sa, t, var, exp);
                    body.add(stmt);
                } else {
                    assert false : "Not yet supported";
                }
            }

            List<Statement> mainBody = new ArrayList<Statement>();

            for (Expression assert_expr : sa.getAssertExprs()) {
                ExtractInputs ei =
                        new ExtractInputs(inputParams, inputTypes, body, varBindings,
                                nres);
                assert_expr = assert_expr.doExpr(ei);
                mainBody.add(new StmtAssert(assert_expr, assert_expr, false));

                // Extract the inductive hypothesis from this assert statement.
                if (assert_expr instanceof ExprBinary) {
                    ExprBinary binExp = (ExprBinary) assert_expr;
                    if (binExp.getOp() == ExprBinary.BINOP_EQ ||
                            binExp.getOp() == ExprBinary.BINOP_TEQ)
                    {
                        Expression lhs = binExp.getLeft();
                        if (lhs instanceof ExprVar) {
                            // TODO: Could be a global variable too
                            String name = ((ExprVar) lhs).getName();
                            lhs = varBindings.get(name);
                            if (lhs == null)
                                throw new ExceptionAtNode(assert_expr,
                                        "Unknown variable " + ((ExprVar) lhs).getName());
                        }
                        Expression rhs = binExp.getRight();
                        if (rhs instanceof ExprVar) {
                            String name = ((ExprVar) rhs).getName();
                            rhs = varBindings.get(name);
                            if (rhs == null)
                                throw new ExceptionAtNode(assert_expr,
                                        "Unknown variable " + ((ExprVar) rhs).getName());
                        }

                        if (lhs instanceof ExprFunCall && rhs instanceof ExprFunCall) {
                            // Things to check
                            // 1. All function names should correspond to valid functions
                            // in the package -
                            // TODO support different packages
                            // 2. f1 and f2 should operate on same type of inputs and
                            // produce same type of
                            // outputs

                            // Assuming here that the optimization is always valid - but
                            // the actual validation check happens in
                            // DisambiguateCallsAndTypeCheck
                            separateHarness = true;
                            if (optimize) {
                                System.out.println("Considering the following invariant " +
                                        rhs.toString() + " == " + lhs.toString());

                                newSpAsserts.add(new StmtSpAssert(
                                        assert_expr.getContext(), (ExprFunCall) rhs,
                                        (ExprFunCall) lhs));
                            }

                            List<Type> lhsRefParamTypes = new ArrayList<Type>();
                            List<Type> rhsRefParamTypes = new ArrayList<Type>();
                            List<Expression> lhsRefParamExprs =
                                    getRefParams((ExprFunCall) lhs, lhsRefParamTypes);
                            List<Expression> rhsRefParamExprs =
                                    getRefParams((ExprFunCall) rhs, rhsRefParamTypes);

                            assert (lhsRefParamTypes.size() == rhsRefParamTypes.size());
                            assert (lhsRefParamTypes.size() == lhsRefParamExprs.size());
                            assert (lhsRefParamExprs.size() == rhsRefParamExprs.size());

                            for (int i = 0; i < lhsRefParamTypes.size(); i++) {
                                assert (lhsRefParamTypes.get(i).equals(rhsRefParamTypes.get(i)));
                            }

                            for (int i = 0; i < lhsRefParamTypes.size(); i++) {
                                assertEqual(lhsRefParamExprs.get(i),
                                        rhsRefParamExprs.get(i), lhsRefParamTypes.get(i),
                                        mainBody);
                            }

                        }
                    }
                } else if (assert_expr instanceof ExprFunCall) {
                    ExprFunCall fc = (ExprFunCall) assert_expr;
                    Function fun = nres.getFun(fc.getName());
                    if (fun.hasAnnotation("replaceable")) {
                        // TODO: Need to do more type checking
                        // Like making sure that the two parameters are of same type.
                        List<Expression> params = fc.getParams();
                        assert (params.size() == 2);
                        ExprFunCall lhs = (ExprFunCall) (params.get(0));
                        ExprFunCall rhs = (ExprFunCall) (params.get(1));
                        List<Expression> wrapperParams = new ArrayList<Expression>();
                        List<Type> wrapperTypes = new ArrayList<Type>();
                        List<Expression> lhsParams = lhs.getParams();
                        List<Expression> newParams = new ArrayList<Expression>();

                        Function lhsFun = nres.getFun(lhs.getName());
                        List<Parameter> actLhsParams = lhsFun.getParams();
                        int i = 0;
                        for (Expression p : lhsParams) {
                            if (p instanceof ExprVar) {
                                String name = ((ExprVar) p).getName();
                                if (varBindings.containsKey(name)) {
                                    Expression rep = varBindings.get(name);
                                    assert (rep instanceof ExprFunCall);
                                    ExprFunCall repFc = (ExprFunCall) rep;
                                    Function repFun =
                                            nres.getFun(((ExprFunCall) rep).getName());
                                    List<Parameter> actRepParams = repFun.getParams();
                                    int j = 0;
                                    for (Expression pp : repFc.getParams()) {
                                        assert (pp instanceof ExprVar);
                                        wrapperParams.add((ExprVar) pp);
                                        wrapperTypes.add(actRepParams.get(j).getType());
                                        j++;
                                    }
                                    newParams.add(rep);
                                } else {
                                    wrapperParams.add((ExprVar) p);
                                    newParams.add(p);
                                    wrapperTypes.add(actLhsParams.get(i).getType());
                                }
                            } else if (p instanceof ExprFunCall) {
                                ExprFunCall repFc = (ExprFunCall) p;
                                Function repFun = nres.getFun(repFc.getName());
                                List<Parameter> actRepParams = repFun.getParams();
                                int j = 0;
                                for (Expression pp : repFc.getParams()) {
                                    assert (pp instanceof ExprVar);
                                    wrapperParams.add((ExprVar) pp);
                                    wrapperTypes.add(actRepParams.get(j).getType());
                                    j++;
                                }
                                newParams.add(p);

                            }
                            i++;
                        }

                        String wrapName = vargen.nextVar("wrapper_" + lhs.getName());
                        Function.FunctionCreator wrapF =
                                Function.creator(sa.getContext(),
                                        wrapName,
                                        Function.FcnType.Static);
                        wrapF.returnType(nres.getFun(lhs.getName()).getReturnType());

                        List<Parameter> wrapParams = new ArrayList<Parameter>();
                        assert (wrapperParams.size() == wrapperTypes.size());

                        for (int k = 0; k < wrapperTypes.size(); k++) {
                            String name = ((ExprVar) wrapperParams.get(k)).getName();
                            wrapParams.add(new Parameter(assert_expr,
                                    wrapperTypes.get(k), name));
                        }

                        wrapF.params(wrapParams);
                        ExprFunCall newLhs =
                                new ExprFunCall(assert_expr, lhs.getName(), newParams);
                        wrapF.body(new StmtBlock(new StmtReturn(assert_expr, newLhs)));

                        Function wrapperFun = wrapF.create();
                        wrapperFun.setPkg(pkg.getName());
                        newFuns.add(wrapperFun);

                        ExprFunCall wrapFunCall =
                                new ExprFunCall(assert_expr, wrapName, wrapperParams);
                        separateHarness = true;
                        if (optimize) {
                        System.out.println(wrapFunCall.toString());
                        System.out.println("Considering the following invariant " +
                                rhs.toString() + " == " + newLhs.toString());

                        newSpAsserts.add(new StmtSpAssert(assert_expr.getContext(),
                                (ExprFunCall) rhs, wrapFunCall));
                        }
                    } 
                } 
            }

            Expression preCond = sa.getPreCond();
            if (preCond != null) {
                StmtIfThen sif =
                        new StmtIfThen(sa, preCond, new StmtBlock(mainBody), null);
                body.add(sif);
            } else {
                body.addAll(mainBody);
            }

            // Add checks for adt inputs because the src tuples can create invalid ADTs.
            for (int i = 0; i < inputTypes.size(); i++) {
                if (inputTypes.get(i).isStruct()) {
                    String fname =
                            createCheckInputFun(sa, (TypeStructRef) inputTypes.get(i),
                                    newFuns);
                    List<Expression> pm = new ArrayList<Expression>();
                    pm.add(new ExprVar(sa, inputParams.get(i)));
                    body.add(0, new StmtIfThen(sa, new ExprFunCall(sa, fname, pm),
                            new StmtEmpty(sa), new StmtReturn(sa, null)));

                } else if (inputTypes.get(i).isArray()) {
                    TypeArray arrType = (TypeArray) (inputTypes.get(i));
                    Type base = arrType.getBase();
                    if (base.isStruct()) {
                        String fname =
                                createCheckInputFun(sa, (TypeStructRef) base,
                                        newFuns);
                        Expression len = arrType.getLength();
                        String idx = vargen.nextVar("i");
                        ExprVar idx_var = new ExprVar(sa, idx);
                        StmtVarDecl init = new StmtVarDecl(sa, TypePrimitive.inttype, idx, ExprConstInt.zero);
                        Expression cond = new ExprBinary(sa, ExprBinary.BINOP_LT, idx_var, len);
                        StmtAssign incr = new StmtAssign(sa, idx_var, new ExprBinary(sa, ExprBinary.BINOP_ADD, idx_var, ExprConstInt.one));
                        List<Expression> pm = new ArrayList<Expression>();
                        pm.add(new ExprArrayRange(sa, new ExprVar(sa, inputParams.get(i)), idx_var));
                        Statement forBody =
                                new StmtBlock(new StmtIfThen(sa, new ExprFunCall(sa,
                                        fname, pm), new StmtEmpty(sa), new StmtReturn(sa,
                                        null)));
                        StmtFor forSt = new StmtFor(sa, init, cond, incr, forBody, true);
                        body.add(0, forSt);
                    }
                }
            }
            
            int splitIdx = inputTypes.size();
            if (separateHarness && !inputTypes.isEmpty()) {
            	// split harnesses on the first ADT input
            	for (int i = 0; i < inputTypes.size(); i++) {
            		if (inputTypes.get(i).isStruct()) {
						splitIdx = i;
            			break;
            		}
            	}
            }
			if (splitIdx < inputTypes.size())
            {
				TypeStructRef ts = (TypeStructRef) (inputTypes.get(splitIdx));
                List<String> cases = getCasesInOrder(ts.getName());
				for (int k = 0; k < cases.size(); k++) {
                    String c = cases.get(k).split("@")[0];
                    Function.FunctionCreator fc =
                            Function.creator(sa.getContext(),
                                    vargen.nextVar("main_" + c), Function.FcnType.Harness);
                    fc.returnType(TypePrimitive.voidtype);

                    List<Parameter> params = new ArrayList<Parameter>();
                    StructDef str = nres.getStruct(c);
                    List<ExprNamedParam> adtParams = new ArrayList<ExprNamedParam>();
                    Map<String, String> varMap = new HashMap<String, String>();

                    List<Statement> new_body = new ArrayList<Statement>();

					for (int i = 0; i < splitIdx; i++) {
						params.add(new Parameter(sa, inputTypes.get(i),
								inputParams.get(i)));
					}

                    for (StructFieldEnt e : str.getFieldEntriesInOrder()) {
                        Type t = e.getType();
                        if (t.isArray() && ((TypeArray) t).getBase().isStruct()) {
                            TypeArray ta = (TypeArray) t;
                            int len = 0;
                            if (ta.getLength().isConstant()) {
                                len = ta.getLength().getIValue();
                            } else {
                                len = arrSize;
                            }
                            List<Expression> exps = new ArrayList<Expression>();
                            for (int i = 0; i < len; i++) {
                                String name = vargen.nextVar(e.getName() + "_" + i);
                                params.add(new Parameter(sa, srcTupleDepth - 1,
                                        ta.getBase(), name));
                                exps.add(new ExprVar(sa, name));
                            }

                            Expression base = new ExprArrayInit(sa, exps);
                            VarReplacer vr = new VarReplacer(varMap);
                            Expression lenExp = ta.getLength().doExpr(vr);
                            Expression range =
                                    new ExprArrayRange(sa, base,
                                            new ExprArrayRange.RangeLen(
                                                    ExprConstInt.zero, lenExp));
                            adtParams.add(new ExprNamedParam(sa, e.getName(), range));
                            Expression cond =
                                    new ExprBinary(ExprBinary.BINOP_GT, lenExp,
                                            new ExprConstInt(len));
                            new_body.add(new StmtIfThen(sa, cond,
                                    new StmtReturn(sa, null), null));
                        } else {
                            String name = vargen.nextVar(e.getName());
                            adtParams.add(new ExprNamedParam(sa, e.getName(),
                                    new ExprVar(sa,
                                name)));
                            int depth = t.isStruct() ? srcTupleDepth - 1 : -1;
                            params.add(new Parameter(sa, depth, t, name));
                            varMap.put(e.getName(), name);
                        }
                    }
                    ExprNew constr =
                            new ExprNew(sa, new TypeStructRef(c, false), adtParams, false);
					StmtVarDecl vd = new StmtVarDecl(sa, ts,
							inputParams.get(splitIdx), constr);
                    new_body.add(vd);
                    new_body.addAll(body);

					for (int i = splitIdx + 1; i < inputTypes.size(); i++) {
                        params.add(new Parameter(sa, inputTypes.get(i),
                                inputParams.get(i)));
                    }
                    fc.params(params);
                    fc.body(new StmtBlock(new_body));

                    Function harness = fc.create();
                    harness.setPkg(pkg.getName());
                    newFuns.add(harness);
                }

            } else {
                Function.FunctionCreator fc =
                        Function.creator(sa.getContext(), vargen.nextVar("main"),
                                Function.FcnType.Harness);
                fc.returnType(TypePrimitive.voidtype);

                List<Parameter> params = new ArrayList<Parameter>();
                for (int i = 0; i < inputTypes.size(); i++) {
                    params.add(new Parameter(sa, inputTypes.get(i), inputParams.get(i)));
                }
                fc.params(params);
                fc.body(new StmtBlock(body));

                Function harness = fc.create();
                harness.setPkg(pkg.getName());
                newFuns.add(harness);
            }
        }

        return new Package(pkg, pkg.getName(), pkg.getStructs(), pkg.getVars(), newFuns,
                newSpAsserts);
    }

    private List<Expression> getRefParams(ExprFunCall exp, List<Type> types) {
        List<Expression> exprs = new ArrayList<Expression>();
        Function fun;
        try {
            fun = nres.getFun(exp.getName(), exp);
        } catch (UnrecognizedVariableException e) {
            throw new ExceptionAtNode(exp, "Unknown function " + exp.getName());
        }

        List<Expression> fparams = exp.getParams();
        List<Parameter> fActParams = fun.getParams();
        if (fparams.size() != fActParams.size())
            throw new ExceptionAtNode(exp, "Wrong number of parameters " + exp.getName());

        if (!fparams.isEmpty()) {
            for (int i = 0; i < fActParams.size(); i++) {
                Parameter p = fActParams.get(i);
                Expression param = fparams.get(i);
                if (p.isParameterReference()) {
                    types.add(p.getType());
                    exprs.add(param);
                }
            }
        }
        return exprs;
    }

    private void assertEqual(Expression e1, Expression e2, Type type, List<Statement> body)
    {
        if (type.isStruct()) {
            Expression cond = new ExprBinary(ExprBinary.BINOP_EQ, e1, new ExprNullPtr());
            Statement ifS = new StmtAssert(new ExprBinary( ExprBinary.BINOP_EQ, e2, new ExprNullPtr()), false);
            Statement elseS = new StmtAssert(new ExprBinary(ExprBinary.BINOP_TEQ, e1, e2), false);
            body.add(new StmtIfThen(e1, cond, ifS, elseS));
        } else if (type.isArray()) {
            TypeArray arrType = (TypeArray) type;
            Type base = arrType.getBase();

            Expression len = arrType.getLength();
            String idx = vargen.nextVar("i");
            ExprVar idx_var = new ExprVar(e1, idx);
            StmtVarDecl init =
                    new StmtVarDecl(e1, TypePrimitive.inttype, idx, ExprConstInt.zero);
            Expression cond = new ExprBinary(e1, ExprBinary.BINOP_LT, idx_var, len);
            StmtAssign incr =
                    new StmtAssign(e1, idx_var, new ExprBinary(e1, ExprBinary.BINOP_ADD,
                            idx_var, ExprConstInt.one));
            Expression lhs = new ExprArrayRange(e1, e1, idx_var);
            Expression rhs = new ExprArrayRange(e1, e2, idx_var);
            Statement forBody;
            if (base.isStruct()) {
                Expression ifcond =
                        new ExprBinary(ExprBinary.BINOP_EQ, lhs, new ExprNullPtr());
                Statement ifS = new StmtAssert(new ExprBinary( ExprBinary.BINOP_EQ, rhs, new ExprNullPtr()), false);
                Statement elseS = new StmtAssert(new ExprBinary(ExprBinary.BINOP_TEQ, lhs, rhs), false);
                forBody = new StmtBlock(new StmtIfThen(e1, ifcond, ifS, elseS));
            } else {
                forBody =
                        new StmtAssert(new ExprBinary(ExprBinary.BINOP_EQ, lhs, rhs),
                                false);
            }
            StmtFor forSt = new StmtFor(e1, init, cond, incr, forBody, true);
            body.add(forSt);

        } else {
            body.add(new StmtAssert(new ExprBinary(ExprBinary.BINOP_EQ, e1, e2),
                    false));
        }
    }

    private String createCheckInputFun(FENode ctx, TypeStructRef t, List<Function> newFuns)
    {
        if (checkFuns.containsKey(t.getName())) {
            return checkFuns.get(t.getName());
        }
        String fname = vargen.nextVar("check" + "_" + t.getName());
        checkFuns.put(t.getName(), fname);
        Function.FunctionCreator fc =
                Function.creator(ctx, fname, Function.FcnType.Static);
        fc.returnType(TypePrimitive.bittype);
        String var = vargen.nextVar("var");
        List<Parameter> params = new ArrayList<Parameter>();
        params.add(new Parameter(ctx, t, var));
        fc.params(params);
        List<Statement> body = new ArrayList<Statement>();
        ExprVar inp = new ExprVar(ctx, var);
        ExprBinary cond =
                new ExprBinary(ctx, ExprBinary.BINOP_EQ, inp,
                        new ExprNullPtr());
        StmtIfThen nullCaseIf =
                new StmtIfThen(ctx, cond, new StmtReturn(ctx, ExprConstInt.one), null);
        body.add(nullCaseIf);

        List<String> orderedCases = getCasesInOrder(t.getName());
        StmtSwitch swt = new StmtSwitch(ctx, inp);
        for (String c : orderedCases) {
            StructDef ts = nres.getStruct(c);
            Expression cur = ExprConstInt.one;
            for (StructFieldEnt fe : ts.getFieldEntriesInOrder()) {
                Type ft = fe.getType();
                if (ft.isStruct()) {
                    String name = createCheckInputFun(ctx, (TypeStructRef) ft, newFuns);
                    Expression rec = new ExprField(ctx, inp, fe.getName());
                    List<Expression> pm = new ArrayList<Expression>();
                    pm.add(rec);
                    ExprBinary recCheck = new ExprBinary(ctx, ExprBinary.BINOP_AND, 
                    		new ExprBinary(ctx, ExprBinary.BINOP_NEQ, rec, new ExprNullPtr()),
                    		new ExprFunCall(ctx, name, pm));
                    cur =
                            new ExprBinary(ctx, ExprBinary.BINOP_AND, recCheck, cur);
                }
            }
            swt.addCaseBlock(c.split("@")[0], new StmtReturn(ctx, cur));
        }
        swt.addCaseBlock("default", new StmtReturn(ctx, ExprConstInt.zero));
        body.add(swt);
        body.add(new StmtReturn(ctx, ExprConstInt.one));

        fc.body(new StmtBlock(body));
        fc.pkg(nres.curPkg().getName());
        newFuns.add(fc.create());

        return fname;
    }

    private List<String> getCasesInOrder(String name) {
        Map<Integer, List<String>> allCases = new HashMap<Integer, List<String>>();
        TypeStructRef type = new TypeStructRef(name, false);

        LinkedList<String> queue = new LinkedList<String>();
        queue.add(name);
        while (!queue.isEmpty()) {
            String n = queue.removeFirst();
            List<String> children = nres.getStructChildren(n);
            if (children.isEmpty()) {
                StructDef ts = nres.getStruct(n);
                int recCount = 0;
                for (StructFieldEnt f : ts.getFieldEntriesInOrder()) {
                    if (f.getType().promotesTo(type, nres))
                        recCount++;
                }
                if (allCases.containsKey(recCount)) {
                    allCases.get(recCount).add(n);
                } else {
                    List<String> cases = new ArrayList<String>();
                    cases.add(n);
                    allCases.put(recCount, cases);
                }

            } else {
                queue.addAll(children);
            }
        }
        Set<Integer> counts = allCases.keySet();
        List<String> orderedCases = new ArrayList<String>();
        for (int c : counts) {
            orderedCases.addAll(allCases.get(c));
        }
        return orderedCases;
    }
}
