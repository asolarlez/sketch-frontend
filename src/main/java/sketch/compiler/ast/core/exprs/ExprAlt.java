/**
 *
 */
package streamit.frontend.nodes;

/**
 * @author <a href="mailto:cgjones@cs.berkeley.edu">Chris Jones</a>
 *
 */
public class ExprAlt extends Expression {
	Expression ths;
	Expression that;

	public ExprAlt (Expression ths, Expression that) {
		super (ths);
		this.ths = ths;
		this.that = that;
	}

	public String toString () {
		return "("+ ths +" | "+ that +")";
	}

	@Override
	public Object accept (FEVisitor v) {
		return null; //v.visitExprAlt (this);
	}

}
