package sketch.transformer;

public class Identifier extends Node {
	String name;

	Identifier(String _name) {
		name = _name;
	}

	public String toString() {
		return name;
	}

	public String get_name() {
		return name;
	}
}
