package sketch.transformer;

public class ReturnLine extends UnitLine {
	Expression expression;

	public ReturnLine(Expression _expression) {
		expression = _expression;
	}

	public String toString() {
		return "return " + expression.toString() + ";";
	}

	@Override
	public void run(State state) {
		// TODO Auto-generated method stub
		assert (false);
	}
}
