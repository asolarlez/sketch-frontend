package sketch.compiler.smt.passes;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.exprs.ExprBinary;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.Expression;

/**
 * This class is for simplifying the AST after EliminateStaticStar
 * The AST after simplification will be much more human-readable
 * and thus easier to verify it's correct
 * 
 * @author Lexin Shan
 * @email lshan@eecs.berkeley.edu
 *
 */
public class ArithmeticSimplification extends FEReplacer {

	@Override
	public Object visitExprBinary(ExprBinary exp) {
		
		Expression left = (Expression) exp.getLeft().accept(this);
		Expression right = (Expression) exp.getRight().accept(this);
		
		if (exp.getOp() == ExprBinary.BINOP_MUL) {
			// 1 * n == n
			if (isConstant(left, 1))
				return right;
			
			if (isConstant(right, 1))
				return left;
			
			// 0 * n = 0
			if (isConstant(right, 0))
				return new ExprConstInt(exp, 0);
			
			if (isConstant(left, 0))
				return new ExprConstInt(exp, 0);
			
			if (left != exp.getLeft() || right != exp.getRight())
				return new ExprBinary(exp, exp.getOp(), left, right);
			
			return exp;
			
			
		} else if (exp.getOp() == ExprBinary.BINOP_ADD) {
			// 0 + n = n
			if (isConstant(left, 0))
				return right;
			
			if (isConstant(right, 0))
				return left;
			
			if (left != exp.getLeft() || right != exp.getRight())
				return new ExprBinary(exp, exp.getOp(), left, right);
			
			return exp;
			
		} else if (exp.getOp() == ExprBinary.BINOP_SUB) {
			// n - 0 = n
			if (isConstant(right, 0))
				return left;
			
			if (left != exp.getLeft() || right != exp.getRight())
				return new ExprBinary(exp, exp.getOp(), left, right);
			
			return exp;
		}
		
		return super.visitExprBinary(exp);
	}
	
	static boolean isConstant(Expression e, int c) {
		if (e instanceof ExprConstInt) {
			ExprConstInt constInt = (ExprConstInt) e;
			return constInt.getVal() == c;
		}
		return false;
	}
}
