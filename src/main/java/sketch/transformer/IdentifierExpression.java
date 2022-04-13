package sketch.transformer;

public class IdentifierExpression extends Expression {
	Identifier identifier;

	IdentifierExpression(Identifier _identifier) {
		identifier = _identifier;
	}

	public String toString() {
		return identifier.toString();
	}

	@Override
	public Param eval(State state) {
		// TODO Auto-generated method stub
		assert (false);
		return null;
	}

	@Override
	public void run(State state) {
		// TODO Auto-generated method stub
		assert (false);
	}
}
