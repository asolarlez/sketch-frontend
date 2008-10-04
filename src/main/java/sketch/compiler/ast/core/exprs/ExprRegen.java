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
		gen = gen.replaceAll(" ", "");
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
		gen = gen.replaceAll(" ", "");
		try{
		this.expr = RegenParser.parse (gen.substring (2, gen.length () - 2),
									   cx);
		}catch(RuntimeException e){
			System.err.println(cx + ": Error Parsing regen " + gen );
			throw e;
		}
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
