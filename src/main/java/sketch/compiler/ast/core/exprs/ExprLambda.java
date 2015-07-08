package sketch.compiler.ast.core.exprs;

import java.util.List;

import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEVisitor;

/**
 * Expression that represents a lambda expression. A lambda expression is of the
 * form
 * 
 * <br>
 * <br>
 * <code>(x, y) -> x + y</code> <br>
 * <br>
 * 
 * The left hand side is a list of ExprVar and the right hand side is an
 * expression.
 * 
 * 
 * @author Miguel Velez
 * @version 0.1
 */
public class ExprLambda extends Expression {

	private List<ExprVar> 	parameters;
	private Expression		expression;

	public ExprLambda(FENode node) {
		super(node);
	}

	/**
	 * Create a new lambda expression by passing context, a variable list, and
	 * an expression.
	 * 
	 * @param context
	 * @param variableList
	 * @param expression
	 */
	public ExprLambda(FEContext context, List<ExprVar> variableList, Expression expression) {
		super(context);
		this.parameters = variableList;
		this.expression = expression;
	}

	/**
	 * Create a new lambda expression by passing context, a variable list, and
	 * an expression
	 * 
	 * @param context
	 * @param variableList
	 * @param expression
	 */
	public ExprLambda(FENode context, List<ExprVar> variableList, Expression expression) {
		super(context);
		this.parameters = variableList;
		this.expression = expression;
	}

	@Override
	public Object accept(FEVisitor visitor) {
		// Visit a lambda expression
		return visitor.visitExprLambda(this);
	}

	/**
	 * Get the parameters of the lambda expression
	 * 
	 * @return
	 */
	public List<ExprVar> getParameters() {
		return this.parameters;
	}

	/**
	 * Set the parameters of the lambda expression
	 * 
	 * @param parameters
	 */
	public void setParameteres(List<ExprVar> parameters) {
		this.parameters = parameters;
	}

	/**
	 * Get the expression from the lambda expression
	 * 
	 * @return
	 */
	public Expression getExpression() {
		return this.expression;
	}

	/**
	 * Set the expression from the lambda expression
	 * 
	 * @param expression
	 */
	public void setExpression(Expression expression) {
		this.expression = expression;
	}

	@Override
	public String toString() {
		return this.parameters.toString() + " -> " + this.expression.toString();
	}

}
