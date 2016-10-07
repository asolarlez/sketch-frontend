package sketch.compiler.passes.bidirectional;

import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.exprs.regens.ExprAlt;
import sketch.compiler.ast.core.typs.Type;

/**
 * This class eliminates the list of fields macro of the form e.{T}
 */
public class FilterRegexChoices extends BidirectionalPass {

	@Override
	public Object visitExprAlt(ExprAlt exp) {
		Type t = driver.tdstate.getExpected();
		Expression ths = exp.getThis();
		Expression that = exp.getThat();
		if (!driver.getType(ths).promotesTo(t, nres())) {
			ths = t.defaultValue();
		}
		if (!driver.getType(that).promotesTo(t, nres())) {
			that = t.defaultValue();
		}
		if (ths != exp.getThis() || that != exp.getThat()) {
			return new ExprAlt(exp, ths, that);
		}
		return exp;
    }

}
