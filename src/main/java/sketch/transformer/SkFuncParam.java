package sketch.transformer;

public class SkFuncParam extends Param {
	Identifier name;

	public SkFuncParam(Identifier _identifier) {
		name = _identifier;
	}

	public Identifier get_identifier() {
		return name;
	}
}
