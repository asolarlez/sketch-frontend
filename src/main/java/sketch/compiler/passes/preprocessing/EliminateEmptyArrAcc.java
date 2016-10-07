package sketch.compiler.passes.preprocessing;

import sketch.compiler.ast.core.exprs.ExprArrayInit;
import sketch.compiler.ast.core.exprs.ExprArrayRange;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.passes.lowering.SymbolTableVisitor;

/**
 * This class eliminates the list of fields macro of the form e.{T}
 */
public class EliminateEmptyArrAcc extends SymbolTableVisitor {

	public EliminateEmptyArrAcc() {
		super(null);
	}

	@Override
	public Object visitExprArrayRange(ExprArrayRange exp) {
		Expression base = exp.getBase();
		if (base instanceof ExprArrayInit) {
			if (((ExprArrayInit) base).getElements().size() == 0) {
				Type t = getType(exp);
				return t.defaultValue();
			}
		}
		return exp;
    }

}
