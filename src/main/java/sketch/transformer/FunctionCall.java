package sketch.transformer;

import java.util.Vector;

public class FunctionCall extends Node {
	Identifier object;

	enum function_name {
		declare, replace, inplace_unit_concretize, unit_clone
	};

	Vector<Param> params;
}
