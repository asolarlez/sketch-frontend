/**
 *
 */
package streamit.frontend.nodes;

import streamit.frontend.parser.RegenParser;

/**
 * @author <a href="mailto:cgjones@cs.berkeley.edu">Chris Jones</a>
 *
 */
public class ExprRegen extends Expression {

	private Expression expr;

	public ExprRegen (FENode cx, String gen) {
		super (cx);
		this.expr = RegenParser.parse (gen.substring (2, gen.length () - 2),
									   cx.getCx ());
	}

	/** @deprecated */
	public ExprRegen (FEContext cx, String gen) {
		super (cx);
		this.expr = RegenParser.parse (gen.substring (2, gen.length () - 2),
									   cx);
	}

	@Override
	public Object accept (FEVisitor v) {
		// TODO Auto-generated method stub
		return null;
	}

}
