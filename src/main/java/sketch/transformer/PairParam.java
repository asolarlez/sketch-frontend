package sketch.transformer;

public class PairParam extends Param {
	Param left;
	Param right;

	public PairParam(Param _left, Param _right) {
		left = _left;
		right = _right;
	}

	Param get_left() {
		return left;
	}

	Param get_right() {
		return right;
	}
}
