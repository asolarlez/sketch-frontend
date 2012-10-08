package sketch.compiler.stencilSK.preprocessor;

import java.util.ArrayList;
import java.util.List;

import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.*;
import sketch.compiler.ast.core.stmts.StmtExpr;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.util.exceptions.ExceptionAtNode;

public class ReplaceFloatsWithFiniteField extends ReplaceFloatsWithBits {

    public final ExprConstInt BASE = ExprConstInt.createConstant(7);

    public final ExprArrayInit DIVTABLE;

    public ReplaceFloatsWithFiniteField(TempVarGen varGen) {
        super(varGen);
        List<Expression> le = new ArrayList<Expression>();
        le.add(ExprConstInt.createConstant(0));
        le.add(ExprConstInt.createConstant(1));
        le.add(ExprConstInt.createConstant(4));
        le.add(ExprConstInt.createConstant(5));
        le.add(ExprConstInt.createConstant(2));
        le.add(ExprConstInt.createConstant(3));
        le.add(ExprConstInt.createConstant(6));
        DIVTABLE = new ExprArrayInit((FENode) null, le);
    }

    public Object visitExprBinary(ExprBinary exp) {
        Type ltype = getType(exp.getLeft());
        Type rtype = getType(exp.getRight());

        if (!isFloat(ltype) && !isFloat(rtype)) {
            return super.visitExprBinary(exp);
        }
        Expression left = doExpression(exp.getLeft());
        Expression right = doExpression(exp.getRight());
        int newOp = exp.getOp();

        switch (exp.getOp()) {
            case ExprBinary.BINOP_ADD:
            case ExprBinary.BINOP_MUL: {
                Expression ebase =
                        new ExprBinary(exp, newOp, left, right, exp.getAlias());
                return new ExprBinary(ebase, "%", BASE);
            }
            case ExprBinary.BINOP_SUB: {
                Expression ebase =
                        new ExprBinary(exp, ExprBinary.BINOP_ADD, left, BASE,
                                exp.getAlias());
                ebase =
                        new ExprBinary(exp, ExprBinary.BINOP_SUB, ebase, right,
                                exp.getAlias());
                return new ExprBinary(ebase, "%", BASE);
            }
            case ExprBinary.BINOP_DIV: {
                Expression ebase = new ExprArrayRange(this.DIVTABLE, right);
                ebase =
                        new ExprBinary(exp, ExprBinary.BINOP_MUL, left, ebase,
                                exp.getAlias());
                ebase = new ExprBinary(ebase, "%", BASE);
                ebase =
                        new ExprTernary(exp, ExprTernary.TEROP_COND, new ExprBinary(
                                right, "!=", ExprConstInt.zero), ebase, ExprConstInt.zero);
                return ebase;
            }
            case ExprBinary.BINOP_EQ:
            case ExprBinary.BINOP_NEQ:
                return new ExprBinary(exp, newOp, left, right, exp.getAlias());
            default:
                assert false : "You can't apply this floating point operation if you are doing floating-point to boolean replacement." +
                        exp + " " + exp.getOp();
        }
        return null;
    }

    public Object visitExprTypeCast(ExprTypeCast exp) {
        Expression expr = doExpression(exp.getExpr());
        Type told = getType(exp);
        Type tnew = (Type) exp.getType().accept(this);
        if (told.equals(TypePrimitive.inttype) && isFloat(tnew)) {
            throw new ExceptionAtNode(
                    "You can't cast from ints to doubles/floats if you are using --fe-fencoding TO_BIT." +
                            exp, exp);
        }
        if (tnew.equals(TypePrimitive.inttype) && isFloat(told)) {
            throw new ExceptionAtNode(
                    "You can't cast from doubles/floats to int if you are using --fe-fencoding TO_BIT." +
                            exp, exp);
        }
        if (expr == exp.getExpr() && tnew == exp.getType())
            return exp;
        else
            return new ExprTypeCast(exp, tnew, expr);
    }

    public Object visitExprConstFloat(ExprConstFloat fexp) {
        String name = null;
        double flc = fexp.getVal();
        if (flc <= 1e-10 && flc >= -1e-10) {
            return ExprConstInt.zero;
        }
        flc = fexp.getVal() - 1.0;
        if (flc <= 1e-10 && flc >= -1e-10) {
            return ExprConstInt.one;
        }
        for (int i = 1; i < BASE.getVal(); ++i) {
            flc = fexp.getVal() - (1.0 / ((double) i));
            if (flc <= 1e-10 && flc >= -1e-10) {
                return DIVTABLE.getElements().get(i);
            }
        }

        Float fl = new Float(fexp.getVal());
        if (floatConstants.containsKey(fl)) {
            name = floatConstants.get(fl).getName();
        } else {
            name = fName(fl);
            floatConstants.put(fl, newFloatFunction(name));
        }
        List<Expression> pl = new ArrayList<Expression>(1);
        ExprVar ev = new ExprVar(fexp, varGen.nextVar());
        pl.add(ev);
        addStatement(new StmtVarDecl(fexp, replType(), ev.getName(), null));
        addStatement(new StmtExpr(new ExprFunCall(fexp, name, pl)));
        return ev;
    }

    public Type replType() {
        return TypePrimitive.inttype;
    }
}
