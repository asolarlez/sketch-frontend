package sketch.transformer;

import java.util.Vector;

public class VectorParam extends Param {
	Vector<Param> params;

	public VectorParam(Vector<Param> _params) {
		params = _params;
	}
}
