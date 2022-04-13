package sketch.transformer;

import java.util.Map;

import sketch.compiler.ast.core.Program;

public class ObjectFunctionCall extends FunctionCall {
	Identifier object_identifier;

	public ObjectFunctionCall(Identifier object_identifier, FunctionCall _function_call) {
		super(_function_call);
		this.object_identifier = object_identifier;
	}

	@Override
	public Param eval(State state) {
		assert (function_name == FunctionName.unit_clone);

		assert (params.size() == 2);

		String skfunc_name = object_identifier.get_name();
		String clone_name = ((StringParam) params.get(0).eval(state)).get_string();
		Map<String, String> hole_rename_map = ((MapParam) params.get(1).eval(state)).get_map_string_to_string();

		System.out.println("in ObjectFunctionCall.eval(state);");

		Cloner cloner = new Cloner(skfunc_name, clone_name, hole_rename_map);

		System.out.println("state.get_program().accept(cloner)");

		Program new_program = (Program) state.get_program().accept(cloner);
		assert (new_program != state.get_program());
		state.set_program(new_program);

		System.out.println("in FunctionCall.eval(state); CLONER PASSED!!!");

		return new SkFuncParam(new Identifier(clone_name));
	}

	@Override
	public void run(State state) {
		assert (function_name == FunctionName.replace || function_name == FunctionName.inplace_unit_concretize);

		if (function_name == FunctionName.replace) {
			assert (params.size() == 2);

			String skfunc_name = object_identifier.get_name();
			String var_name = ((StringParam) params.get(0).eval(state)).get_string();
			String new_val = ((SkFuncParam) params.get(1).eval(state)).get_identifier().get_name();

			System.out.println("in ObjectFunctionCall.run(state);");

			Replacer replacer = new Replacer(skfunc_name, var_name, new_val);

			System.out.println("state.get_program().accept(replacer)");

			Program new_program = (Program) state.get_program().accept(replacer);
			assert (new_program != state.get_program());
			state.set_program(new_program);


			System.out.println("in FunctionCall.run(state); REPLACER PASSED!!!");

			return;

		} else if (function_name == FunctionName.inplace_unit_concretize) {

			assert (params.size() == 1);

			String skfunc_name = object_identifier.get_name();
			Map<String, String> hole_vals = ((MapParam) params.get(0).eval(state)).get_map_string_to_string();

			System.out.println("in ObjectFunctionCall.run(state);");

			Concretizer concretizer = new Concretizer(skfunc_name, hole_vals, state.get_program());

			System.out.println("state.get_program().accept(concretizer): " + skfunc_name);

			Program new_program = (Program) state.get_program().accept(concretizer);
			assert (new_program != state.get_program());
			state.set_program(new_program);

			System.out.println("in FunctionCall.run(state); CONCRETIZER PASSED!!!");

			return;

		} else {
			// ERROR OR NEED TO EXTEND.
			assert (false);
		}

		// ERROR OR NEED TO EXTEND.
		assert (false);
	}

	public String toString() {
		return object_identifier.toString() + "." + super.toString();
	}
}

