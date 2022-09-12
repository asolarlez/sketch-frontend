package sketch.transformer;

public class IdentifierExpression extends Expression {
	IdentifierParam identifier;

	IdentifierExpression(IdentifierParam _identifier) {
		identifier = _identifier;
	}

	public String toString() {
		return identifier.toString();
	}

	@Override
	public Param eval(State state) {
		return identifier.eval(state);
	}

	@Override
	public void run(State state) {
		// TODO Auto-generated method stub
		assert (false);
	}
}
