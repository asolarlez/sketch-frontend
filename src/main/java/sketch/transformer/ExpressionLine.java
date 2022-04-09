package sketch.transformer;

public class ExpressionLine extends UnitLine {

	private Expression expression;
	
	public ExpressionLine(Expression _expression) {
		expression = _expression;
	}

	public String toString() {
		return expression.toString();
	}
}
