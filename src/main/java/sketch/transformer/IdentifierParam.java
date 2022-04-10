package sketch.transformer;

public class IdentifierParam extends Param {
	Identifier identifier;

	public IdentifierParam(Identifier _identifier) {
		identifier = _identifier;
	}

	public String toString() {
		return identifier.toString();
	}

	Identifier get_identifier() {
		return identifier;
	}
}
