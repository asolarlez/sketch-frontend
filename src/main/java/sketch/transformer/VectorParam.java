package sketch.transformer;

import java.util.Vector;

public class VectorParam extends Param {
	Vector<Param> params;

	public VectorParam(Vector<Param> _params) {
		params = _params;
	}

	public String toString() {
		String ret = "[";
		for (int i = 0; i < params.size(); i++) {
			if (i >= 1) {
				ret += ", ";
			}
			ret += params.get(i).toString();
		}
		ret += "]";
		return ret;
	}

	public Vector<String> get_vector_of_strings() {
		Vector<String> ret = new Vector<String>();
		for (int i = 0; i < params.size(); i++) {
			ret.add(((StringParam) params.get(i)).get_string());
		}
		return ret;
	}
}
