package sketch.compiler.passes.bidirectional;

import sketch.compiler.ast.core.exprs.ExprArrayRange;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.exprs.regens.ExprAlt;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;

/**
 * This class eliminates the list of fields macro of the form e.{T}
 */
public class FilterRegexChoices extends BidirectionalPass {

	@Override
	public Object visitExprAlt(ExprAlt exp) {
		Type t = driver.tdstate.getExpected();
		Expression ths = exp.getThis();
		Expression that = exp.getThat();
		Type left = driver.getType(ths);
		if (!left.promotesTo(t, nres())) {
			ths = t.defaultValue();
		}
		if (t.isArray() && left.isArray()) { // TODO: check this
			ths = new ExprArrayRange(ths, ths, 
           		 new ExprArrayRange.RangeLen(ExprConstInt.zero, ((TypeArray) t).getLength()));
		}
		Type right = driver.getType(that);
		if (!right.promotesTo(t, nres())) {
			that = t.defaultValue();
		}
		if (t.isArray() && right.isArray()) { // TODO: check this
			that = new ExprArrayRange(that, that, 
           		 new ExprArrayRange.RangeLen(ExprConstInt.zero, ((TypeArray) t).getLength()));
		}
		if (ths != exp.getThis() || that != exp.getThat()) {
			return new ExprAlt(exp, ths, that);
		}
		return exp;
    }

}
