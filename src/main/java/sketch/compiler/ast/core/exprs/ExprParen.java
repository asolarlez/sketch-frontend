/**
 *
 */
package streamit.frontend.nodes;

/**
 * @author <a href="mailto:cgjones@cs.berkeley.edu">Chris Jones</a>
 */
public class ExprParen extends Expression {
	private Expression expr;

	public ExprParen (Expression expr) {
		super (expr);
		this.expr = expr;
	}

	public Expression getExpr () {
		return expr;
	}

	public String toString () {
		return "("+ expr +")";
	}

	@Override
	public Object accept (FEVisitor v) {
		// TODO Auto-generated method stub
		return null;
	}

}
