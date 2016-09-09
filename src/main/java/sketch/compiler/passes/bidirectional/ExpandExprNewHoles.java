package sketch.compiler.passes.bidirectional;

import sketch.compiler.ast.core.exprs.ExprNew;
import sketch.compiler.ast.core.exprs.ExprStar;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.util.exceptions.ExceptionAtNode;

/**
 * Expands field selector holes into a set of ExprFields based on the type from the
 * context.
 */
public class ExpandExprNewHoles extends BidirectionalPass {
	@Override
	public Object visitExprNew(ExprNew exp) {
		Type expType = driver.tdstate.getExpected();
		if (exp.isHole() && exp.getTypeToConstruct() == null) {
			if (expType != null && expType.isStruct()) {
				ExprStar star = new ExprStar(exp, 5, TypePrimitive.int32type);
				exp = new ExprNew(exp.getContext(), expType, exp.getParams(),
						true);
				exp.setStar(star);
				expType = null;
			} else {
				throw new ExceptionAtNode("Type must be of type struct", exp);
			}
		}
		return exp;
	}
}
