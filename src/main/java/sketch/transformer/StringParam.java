package sketch.transformer;

public class StringParam extends Param {
	String str;

	public StringParam(String _str) {
		str = _str;
	}

	public String get_string() {
		return str;
	}

	public String toString() {
		return str;
	}
}
