package sketch.transformer;

public class IdentifierExpression extends Expression {
	Identifier identifier;

	IdentifierExpression(Identifier _identifier) {
		identifier = _identifier;
	}

	public String toString() {
		return identifier.toString();
	}
}
