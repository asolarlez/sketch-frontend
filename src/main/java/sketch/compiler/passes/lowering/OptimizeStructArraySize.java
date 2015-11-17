package sketch.compiler.passes.lowering;

import sketch.compiler.ast.core.exprs.ExprBinary;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprField;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.typs.StructDef;
import sketch.compiler.ast.core.typs.StructDef.StructFieldEnt;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypeStructRef;

// Replace y < p.n to y < maxArrSize && y < p.n
public class OptimizeStructArraySize extends SymbolTableVisitor {
	final int maxArrSize;

	public OptimizeStructArraySize(int maxArrSize) {
		super(null);
		this.maxArrSize = maxArrSize;
	}

	@Override
	public Object visitExprBinary(ExprBinary exp) {
		if (exp.getOp() == ExprBinary.BINOP_LT) {
			if (isStructArrSize(exp.getRight())) {
				return new ExprBinary(exp, ExprBinary.BINOP_AND,
						new ExprBinary(exp, ExprBinary.BINOP_LT, exp.getLeft(),
								new ExprConstInt(maxArrSize)), exp);
			}
		}
		return super.visitExprBinary(exp);
	}

	private boolean isStructArrSize(Expression exp) {
		if (exp instanceof ExprField) {
			ExprField ef = (ExprField) exp;
			String fname = ef.getName();
			Type t = getType(ef.getLeft());
			assert (t.isStruct());
			StructDef ts = nres.getStruct(((TypeStructRef) t).getName());
			for (StructFieldEnt s : ts.getFieldEntriesInOrder()) {
				if (s.getType().isArray()) {
					TypeArray ta = (TypeArray) s.getType();
					Expression len = ta.getLength();
					if (len instanceof ExprVar
							&& ((ExprVar) len).getName().equals(fname)) {
						return true;
					}
				}
			}

		}
		return false;
	}
}
