package sketch.transformer;

import java.util.Vector;

public class FunctionCall extends Node {


	enum function_name {
		declare, replace, inplace_unit_concretize, unit_clone
	};

	Identifier function_name;

	Vector<Param> params;

	public FunctionCall(FunctionCall _function_call) {
		function_name = _function_call.function_name;
		params = _function_call.params;
	}

	public FunctionCall(Identifier _identifier, Vector<Param> _params) {
		function_name = _identifier;
		params = _params;
	}
}
