/**
 *
 */
package streamit.frontend.nodes;

import streamit.frontend.parser.RegenParser;

/**
 * @author <a href="mailto:cgjones@cs.berkeley.edu">Chris Jones</a>
 */
public class ExprRegen extends Expression {

	private Expression expr;

	public ExprRegen (FENode cx, String gen) {
		super (cx);
		this.expr = RegenParser.parse (gen.substring (2, gen.length () - 2),
									   cx.getCx ());
	}

	public ExprRegen (FENode cx, Expression expr) {
		super (cx);
		this.expr = expr;
	}

	/** @deprecated */
	public ExprRegen (FEContext cx, String gen) {
		super (cx);
		this.expr = RegenParser.parse (gen.substring (2, gen.length () - 2),
									   cx);
	}

	public Expression getExpr () { return expr; }

	public boolean isLValue () {
		return expr.isLValue ();
	}

	public String toString () {
		return "{| "+ expr +" |}";
	}

	@Override
	public Object accept (FEVisitor v) {
		return v.visitExprRegen (this);
	}

}
