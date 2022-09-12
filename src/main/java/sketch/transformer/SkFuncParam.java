package sketch.transformer;

public class SkFuncParam extends Param {
	IdentifierParam name;

	public SkFuncParam(IdentifierParam _identifier) {
		name = _identifier;
	}

	public Identifier get_identifier() {
		return name.get_identifier();
	}
}
