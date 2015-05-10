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
import sketch.compiler.ast.core.Package;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.*;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssert;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.stmts.StmtExpr;
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
import sketch.util.exceptions.ExceptionAtNode;
import sketch.util.exceptions.UnrecognizedVariableException;

public class CreateHarnesses extends FEReplacer {
    Map<String, String> produceFuns = new HashMap<String, String>();
    TempVarGen vargen;

    public CreateHarnesses(TempVarGen varGen) {
        this.vargen = varGen;
    }

    @Override
    public Object visitPackage(Package pkg) {
        nres.setPackage(pkg);

        List<Function> newFuns = pkg.getFuncs();
        for (StmtSpAssert sa : pkg.getSpAsserts()) {
            // Things to check
            // 1. All function names should correspond to valid functions in the package -
            // TODO support different packages
            // 2. f1 and f2 should operate on same type of inputs and produce same type of
            // outputs

            ExprFunCall f1 = sa.getFirstFun();
            ExprFunCall f2 = sa.getSecondFun();

            Function fun1, fun2;
            try {
                fun1 = nres.getFun(f1.getName(), f1);
            } catch (UnrecognizedVariableException e) {
                throw new ExceptionAtNode(f1, "unknown function " + f1.getName());
            }
            try {
                fun2 = nres.getFun(f2.getName(), f2);
            } catch (UnrecognizedVariableException e) {
                throw new ExceptionAtNode(f2, "unknown function " + f2.getName());
            }

            List<Expression> f1params = f1.getParams();
            List<Expression> f2params = f2.getParams();

            List<Parameter> f1ActParams = fun1.getParams();
            List<Parameter> f2ActParams = fun2.getParams();

            List<ExprVar> inparams = new ArrayList<ExprVar>();

            List<Type> intypes = new ArrayList<Type>();
            List<Type> outtypes = new ArrayList<Type>();

            List<ExprVar> inparams2 = new ArrayList<ExprVar>();

            List<Type> intypes2 = new ArrayList<Type>();
            List<Type> outtypes2 = new ArrayList<Type>();

            if (!f1params.isEmpty()) {
                if (f1params.get(0) instanceof ExprFunCall) {
                    ExprFunCall f3 = (ExprFunCall) f1params.get(0);
                    List<Expression> f3params = f3.getParams();
                    Function fun3;
                    try {
                        fun3 = nres.getFun(f3.getName(), f3);
                    } catch (UnrecognizedVariableException e) {
                        throw new ExceptionAtNode(f3, "unknown function " + f3.getName());
                    }

                    List<Parameter> f3ActParams = fun3.getParams();
                    for (int i = 0; i < f3ActParams.size(); i++) {
                        Parameter p = f3ActParams.get(i);
                        if (p.isParameterInput()) {
                            inparams.add((ExprVar) f3params.get(i));
                            intypes.add(p.getType());
                        }
                        if (p.isParameterOutput()) {
                            outtypes.add(p.getType());
                        }
                    }

                    for (int i = 1; i < f1ActParams.size(); i++) {
                        Parameter p = f1ActParams.get(i);
                        if (p.isParameterInput()) {
                            inparams.add((ExprVar) f1params.get(i));
                            intypes.add(p.getType());
                        }
                        if (p.isParameterOutput()) {
                            outtypes.add(p.getType());
                        }
                    }

                } else {

                    for (int i = 0; i < f1ActParams.size(); i++) {
                        Parameter p = f1ActParams.get(i);
                        if (p.isParameterInput()) {
                            inparams.add((ExprVar) f1params.get(i));
                            intypes.add(p.getType());
                        }
                        if (p.isParameterOutput()) {
                            outtypes.add(p.getType());
                        }
                    }
                }

            }

            outtypes.add(fun1.getReturnType());

            for (int i = 0; i < f2ActParams.size(); i++) {
                Parameter p = f2ActParams.get(i);
                if (p.isParameterInput()) {
                    inparams2.add((ExprVar) f2params.get(i));
                    intypes2.add(p.getType());
                }
                if (p.isParameterOutput()) {
                    outtypes2.add(p.getType());
                }
            }
            outtypes2.add(fun2.getReturnType());

            if (intypes.size() != intypes2.size()) {
                throw new ExceptionAtNode(sa, "Inputs sizes are different");
            }

            if (outtypes.size() != outtypes2.size()) {
                throw new ExceptionAtNode(sa, "Outputs sizes are different");
            }

            for (int i = 0; i < intypes.size(); i++) {
                if (!intypes.get(i).equals(intypes2.get(i))) {
                    throw new ExceptionAtNode(sa, "Input types are different");
                }

                if (!inparams.get(i).equals(inparams2.get(i))) {
                    throw new ExceptionAtNode(sa, "Input params are different");
                }
            }

            for (int i = 0; i < outtypes.size(); i++) {
                if (!outtypes.get(i).equals(outtypes2.get(i))) {
                    throw new ExceptionAtNode(sa, "Output types are different");
                }
            }

            Function harness = createHarness(sa, inparams, intypes, outtypes, newFuns);
            harness.setPkg(pkg.getName());
            newFuns.add(harness);

        }

        return new Package(pkg, pkg.getName(), pkg.getStructs(), pkg.getVars(), newFuns,
                pkg.getSpAsserts());
    }

    private Function createHarness(StmtSpAssert sa, List<ExprVar> inparams,
            List<Type> intypes, List<Type> outtypes, List<Function> newFuns)
    {
        Function.FunctionCreator fc =
                Function.creator(sa.getContext(), "main", Function.FcnType.Harness); // TODO: more general naming
        fc.returnType(TypePrimitive.voidtype);

        List<Parameter> params = new ArrayList<Parameter>();

        for (int i = 0; i < intypes.size(); i++) {
            Type t = intypes.get(i);
            if (t.isStruct()) {
                Parameter p =
                        new Parameter(sa, new TypeArray(TypePrimitive.inttype,
                                ExprConstInt.createConstant(30)), // TODO: eliminate this magic number
                                inparams.get(i).getName() + "_arr");
                params.add(p);
            } else {
                params.add(new Parameter(sa, intypes.get(i), inparams.get(i).getName()));
            }
        }
        
        fc.params(params);
        
        List<Statement> body = new ArrayList<Statement>();
        
        for (int i = 0; i < intypes.size(); i++) {
            Type t = intypes.get(i);
            if (t.isStruct()) {
                String funName = createFunction((TypeStructRef) t, sa, newFuns);
                List<Expression> pm = new ArrayList<Expression>();
                pm.add(new ExprVar(sa, inparams.get(i).getName() + "_arr"));
                pm.add(ExprConstInt.createConstant(2)); // TODO: Magic number
                String tmp = vargen.nextVar();
                body.add(new StmtVarDecl(sa, TypePrimitive.inttype, tmp,
                        ExprConstInt.zero));
                pm.add(new ExprVar(sa, tmp));

                body.add(new StmtVarDecl(sa, t, inparams.get(i).getName(),
                        new ExprFunCall(sa, funName, pm)));
            }
        }
        String newvar = vargen.nextVar();
        body.add(new StmtVarDecl(sa, outtypes.get(outtypes.size() - 1), newvar,
                sa.getSecondFun()));

        ExprBinary cond =
                new ExprBinary(sa, ExprBinary.BINOP_NEQ, new ExprVar(sa, newvar),
                        new ExprNullPtr());
        StmtAssert assertStmt =
                new StmtAssert(sa, new ExprBinary(sa, ExprBinary.BINOP_TEQ, new ExprVar(
                        sa, newvar), sa.getFirstFun()), false);
        StmtIfThen sif = new StmtIfThen(sa, cond, assertStmt, null);

        body.add(sif);

        fc.body(new StmtBlock(body));

        return fc.create();
    }

    private String createFunction(TypeStructRef t, FENode ctx, List<Function> newFuns) {
        if (produceFuns.containsKey(t.getName())) {
            return produceFuns.get(t.getName());
        }
        String fname = "produce" + "_" + t.getName(); // TODO: more general naming
        produceFuns.put(t.getName(), fname);
        Function.FunctionCreator fc =
                Function.creator(ctx, fname, Function.FcnType.Static);
        fc.returnType(t);
        String arr = vargen.nextVar("arr");
        String bnd = vargen.nextVar("bnd");
        String idx = vargen.nextVar("idx");
        List<Parameter> params = new ArrayList<Parameter>();
        params.add(new Parameter(ctx, new TypeArray(TypePrimitive.inttype,
                ExprConstInt.createConstant(30)), // TODO: eliminate this magic number
                arr));

        params.add(new Parameter(ctx, TypePrimitive.inttype, bnd));
        params.add(new Parameter(ctx, TypePrimitive.inttype, idx, Parameter.REF));

        fc.params(params);

        List<Statement> body = new ArrayList<Statement>();
        ExprBinary cond =
                new ExprBinary(ctx, ExprBinary.BINOP_LE, new ExprVar(ctx, bnd),
                        ExprConstInt.zero);
        StmtIfThen baseCaseIf =
                new StmtIfThen(ctx, cond, new StmtReturn(ctx, new ExprNullPtr()), null);
        body.add(baseCaseIf);
        List<String> orderedCases = getCasesInOrder(t.getName());

        if (orderedCases.size() == 0) {
            body.add(new StmtReturn(ctx, new ExprNullPtr()));
        } else {

            Statement cur =
                    genCase(orderedCases.get(orderedCases.size() - 1), arr, bnd, idx,
                            ctx, newFuns);

        for (int i = orderedCases.size() - 2; i >= 0; i--) {
            Expression cnd =
                    new ExprBinary(ctx, ExprBinary.BINOP_EQ, new ExprArrayRange(
                            new ExprVar(ctx, arr), new ExprVar(ctx, idx)),
                            ExprConstInt.createConstant(i));
                Statement ifPart =
                        genCase(orderedCases.get(i), arr, bnd, idx, ctx, newFuns);
            cur = new StmtIfThen(ctx, cnd, ifPart, cur);
        }
            body.add(cur);
        }

        fc.body(new StmtBlock(body));
        fc.pkg(nres.curPkg().getName());
        newFuns.add(fc.create());

        return fname;

    }

    private Statement genCase(String name, String arr, String bnd, String idx,
            FENode ctx, List<Function> newFuns)
    {
        List<Statement> stmts = new ArrayList<Statement>();
        // First increment the idx
        stmts.add(new StmtExpr(ctx, new ExprUnary(ctx, ExprUnary.UNOP_POSTINC,
                new ExprVar(ctx, idx))));

        List<ExprNamedParam> params = new ArrayList<ExprNamedParam>();
        // TODO: should actually create params for all fields in this struct and its
        // parents
        StructDef ts = nres.getStruct(name);

        for (StructFieldEnt e : ts.getFieldEntriesInOrder()) {
            Expression arg = null;
            Type t = e.getType();
            Expression arr_acc = new ExprArrayRange(new ExprVar(ctx,arr), new ExprUnary(ctx, ExprUnary.UNOP_POSTINC, new ExprVar(ctx, idx)));
            if (t.promotesTo(TypePrimitive.bittype, nres)) {
                arg =
                        new ExprBinary(ctx, ExprBinary.BINOP_EQ, arr_acc,
                                ExprConstInt.zero);
            } else if (t.promotesTo(TypePrimitive.inttype, nres)) {
                arg = arr_acc;
            } else if (t.isStruct()) {
                String funName = createFunction((TypeStructRef) t, ctx, newFuns);
                List<Expression> pm = new ArrayList<Expression>();
                pm.add(new ExprVar(ctx, arr));
                pm.add(new ExprBinary(ctx, ExprBinary.BINOP_SUB, new ExprVar(ctx, bnd),
                        ExprConstInt.one));
                pm.add(new ExprVar(ctx, idx));
                arg = new ExprFunCall(ctx, funName, pm);
            } else {
                assert (false); // TODO: also deal with array fields
            }
            params.add(new ExprNamedParam(ctx, e.getName(), arg));
        }

        ExprNew node = new ExprNew(ctx, new TypeStructRef(name, false), params, false);

        stmts.add(new StmtReturn(ctx, node));
        return new StmtBlock(stmts);
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
