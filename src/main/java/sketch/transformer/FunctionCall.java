package sketch.transformer;

import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import sketch.compiler.ast.core.Program;

public class FunctionCall extends Node {

	enum FunctionName {
		declare, replace, inplace_unit_concretize, unit_clone
	};

	private static Map<String, FunctionName> str_to_function_name = new TreeMap<String, FunctionName>();
	static {
		str_to_function_name.put("declare", FunctionName.declare);
		str_to_function_name.put("replace", FunctionName.replace);
		str_to_function_name.put("inplace_unit_concretize", FunctionName.inplace_unit_concretize);
		str_to_function_name.put("unit_clone", FunctionName.unit_clone);
	}

	FunctionName function_name;

	Vector<Param> params;

	public FunctionCall(FunctionCall _function_call) {
		function_name = _function_call.function_name;
		params = _function_call.params;
	}

	public FunctionCall(Identifier _identifier, Vector<Param> _params) {
		assert (str_to_function_name.containsKey(_identifier.get_name()));
		function_name = str_to_function_name.get(_identifier.get_name());
		params = _params;
	}

	public String toString() {
		String ret;
		ret = function_name + "(";
		for (int i = 0; i < params.size(); i++) {
			if (i >= 1) {
				ret += ", ";
			}
			ret += params.get(i).toString();
		}
		ret += ")";
		return ret;
	}

	public Param eval(State state) {
		assert (function_name == FunctionName.declare);

		assert (params.size() == 3);

		String skfunc_name = ((StringParam) params.get(0).eval(state)).get_string();
		Vector<String> hole_names = ((VectorParam) params.get(1).eval(state)).get_vector_of_strings();
		Map<String, String> port_var_to_port_val = ((MapParam) params.get(2).eval(state)).get_map_string_to_string();

		// to call Declarer (which actually just asserts that the

		System.out.println("in FunctionCall.eval(state);");

		Declarer declarer = new Declarer(skfunc_name, hole_names, port_var_to_port_val);

		System.out.println("state.get_program().accept(declarer)");

		Program new_program = (Program) state.get_program().accept(declarer);
		assert (new_program != state.get_program());
		state.set_program(new_program);

		System.out.println("in FunctionCall.eval(state); DECLARER PASSED!!!");

		return new SkFuncParam(new Identifier(skfunc_name));
	}

	public void run(State state) {
		// no need.
		assert (false);
	}

}
