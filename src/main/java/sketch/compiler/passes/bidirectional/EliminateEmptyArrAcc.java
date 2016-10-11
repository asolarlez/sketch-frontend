package sketch.compiler.passes.bidirectional;

import sketch.compiler.ast.core.exprs.ExprArrayInit;
import sketch.compiler.ast.core.exprs.ExprArrayRange;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.typs.NotYetComputedType;
import sketch.compiler.ast.core.typs.Type;

/**
 * This class eliminates the list of fields macro of the form e.{T}
 */
public class EliminateEmptyArrAcc extends BidirectionalPass {

	@Override
	public Object visitExprArrayRange(ExprArrayRange exp) {
		Expression base = exp.getBase();
		if (base instanceof ExprArrayInit) {
			if (((ExprArrayInit) base).getElements().size() == 0) {
				Type t = driver.tdstate.getExpected();
				if ((t instanceof NotYetComputedType))
					return ExprConstInt.zero;
				return t.defaultValue();
			}
		}
		return exp;
    }

}
