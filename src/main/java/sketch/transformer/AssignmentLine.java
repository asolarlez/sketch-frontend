package sketch.transformer;

public class AssignmentLine extends UnitLine {
	Identifier identifier;
	Expression expression;

	public AssignmentLine(Identifier _identifier, Expression _expression) {
		super();
		identifier = _identifier;
		expression = _expression;
	}

	public String toString() {
		return identifier.toString() + " = " + expression.toString() + ";";
	}

	@Override
	public void run(State state) {
		state.add(identifier, expression.eval(state));
	}
}
