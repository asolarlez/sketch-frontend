package sketch.compiler.smt.partialeval;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.exprs.ExprBinary;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypePrimitive;

public class BitVectUtil {
	
	static ConstExprEval eval = new ConstExprEval();
	
	private static class ConstExprEval extends FEReplacer {
		private int mResult;
		@Override
		public Expression visitExprBinary(ExprBinary exp) {
			int op = exp.getOp();
			exp.getLeft().accept(this);
			int left = mResult; 
			exp.getRight().accept(this);
			int right = mResult;
			
			switch (op) {
			case ExprBinary.BINOP_ADD :
				mResult = left + right;
				
			case ExprBinary.BINOP_SUB :
				mResult = left - right;
				
			case ExprBinary.BINOP_MUL :
				mResult = left * right;
				
			case ExprBinary.BINOP_DIV :
				mResult = left / right;
			}
			return exp;
		}
		
		@Override
		public Expression visitExprConstInt(ExprConstInt exp) {
			mResult = exp.getIValue();
			return exp; 
		}
	}
	
	
	public static boolean isBitArray(Type t) {
		return (t instanceof TypeArray) && 
		(((TypeArray) t).getBase() == TypePrimitive.bittype);
	
	}
	
	public static int vectSize(Type t) {
		if (t.equals(TypePrimitive.bittype))
			return 1;
		TypeArray ta = (TypeArray) t;
		ta.getLength().accept(eval);
		return eval.mResult; 
	}
	
	public static Type newBitArrayType(int size) {
		return new TypeArray(TypePrimitive.bittype, new ExprConstInt(size));
	}
	
	public static boolean isPrimitive(Type t) {
		
		return t instanceof TypePrimitive || BitVectUtil.isBitArray(t);
	}
}
