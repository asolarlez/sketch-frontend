/**
 *
 */
package sketch.compiler.ast.core.exprs.regens;
import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEVisitor;
import sketch.compiler.ast.core.exprs.Expression;

/**
 * Parentheses expression of the form "( expr )"
 * 
 * @author <a href="mailto:cgjones@cs.berkeley.edu">Chris Jones</a>
 */
public class ExprParen extends Expression {
	private Expression expr;

	public ExprParen (Expression expr) {
		this (expr, expr);
	}

	public ExprParen (FENode cx, Expression expr) {
		super (cx);
		this.expr = expr;
	}

	public Expression getExpr () {
		return expr;
	}

	@Override public boolean isLValue () {
		return expr.isLValue ();
	}

	public String toString () {
		return "("+ expr +")";
	}

	@Override
	public Object accept (FEVisitor v) {
		return v.visitExprParen (this);
	}

}
