package sketch.transformer;

public class StringParam extends Param {
	private String str;

	public StringParam(String _str) {
		assert (_str.charAt(0) == '"');
		assert (_str.charAt(_str.length() - 1) == '"');
		str = _str.substring(1, _str.length() - 1);
	}

	public String get_string() {
		return str;
	}

	public String toString() {
		return "\"" + str + "\"";
	}
}
