package sketch.compiler.stencilSK.preprocessor;

import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.ExprBinary;
import sketch.compiler.ast.core.exprs.ExprConstFloat;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprTypeCast;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypePrimitive;

public class ReplaceFloatsWithFixpoint extends ReplaceFloatsWithBits {

    public static int scale = 100;

    public ReplaceFloatsWithFixpoint(TempVarGen varGen) {
        super(varGen);
    }

    public Object visitExprConstFloat(ExprConstFloat fexp) {
        return ExprConstInt.createConstant((int) (fexp.getVal() * scale));
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
            case ExprBinary.BINOP_SUB:
            case ExprBinary.BINOP_GE:
            case ExprBinary.BINOP_LE:
            case ExprBinary.BINOP_GT:
            case ExprBinary.BINOP_LT:
            case ExprBinary.BINOP_EQ:
            case ExprBinary.BINOP_NEQ: {
                if (left == exp.getLeft() && right == exp.getRight() &&
                        newOp == exp.getOp())
                    return exp;
                else
                    return new ExprBinary(exp, newOp, left, right, exp.getAlias());
            }
            case ExprBinary.BINOP_MUL: {
                Expression ebase =
                        new ExprBinary(exp, ExprBinary.BINOP_MUL, left, right,
                                exp.getAlias());
                ebase =
                        new ExprBinary(exp, ExprBinary.BINOP_DIV, ebase,
                                ExprConstInt.createConstant(scale),
                                exp.getAlias());
                return ebase;
            }
            case ExprBinary.BINOP_DIV: {
                Expression ebase =
                        new ExprBinary(exp, ExprBinary.BINOP_MUL, left,
                                ExprConstInt.createConstant(scale), exp.getAlias());
                ebase =
                        new ExprBinary(exp, ExprBinary.BINOP_DIV, ebase, right,
                                exp.getAlias());
                return ebase;
            }

            default:
                assert false : "You can't apply this floating point operation if you are doing floating-point to boolean replacement." +
                        exp + " " + exp.getOp();
        }
        return null;
    }

    public Object visitExprTypeCast(ExprTypeCast exp) {
        Expression expr = doExpression(exp.getExpr());
        Type told = getType(exp.getExpr());
        Type tnew = exp.getType();
        if (told.equals(TypePrimitive.inttype) && isFloat(tnew)) {
            return new ExprBinary(expr, "*", ExprConstInt.createConstant(scale));
        }
        if (tnew.equals(TypePrimitive.inttype) && isFloat(told)) {
            return new ExprBinary(expr, "/", ExprConstInt.createConstant(scale));
        }
        if (expr == exp.getExpr() && tnew == exp.getType())
            return exp;
        else
            return new ExprTypeCast(exp, tnew, expr);
    }

    public Type replType() {
        return TypePrimitive.inttype;
    }

}
