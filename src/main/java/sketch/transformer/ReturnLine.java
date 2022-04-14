package sketch.transformer;

import java.util.Set;
import java.util.TreeSet;

import sketch.compiler.ast.core.Program;

public class ReturnLine extends UnitLine {
	Expression expression;

	public ReturnLine(Expression _expression) {
		expression = _expression;
	}

	public String toString() {
		return "return " + expression.toString() + ";";
	}

	Set<String> get_subfunctions(String root_skfunc_name, Program program) {

		int prev_ret_size = 0;

		Set<String> ret = new TreeSet<String>();
		ret.add(root_skfunc_name);

		while (prev_ret_size != ret.size()) {
			prev_ret_size = ret.size();
			FunctionVisitor function_visitor = new FunctionVisitor(ret);
			program.accept(function_visitor);
		}

		return ret;
	}

	@Override
	public void run(State state) {

		String skfunc_name = ((SkFuncParam) expression.eval(state)).get_identifier().get_name();

		System.out.println("in ReturnLine.run(state);");

		Set<String> subfuncs = get_subfunctions(skfunc_name, state.get_program());

		for (String it : subfuncs) {
			System.out.println(it);
		}

		Slicer sliceer = new Slicer(subfuncs);

		System.out.println("state.get_program().accept(sliceer)");

		Program new_program = (Program) state.get_program().accept(sliceer);
		assert (new_program != state.get_program());
		state.set_return(new_program);

		System.out.println("in ReturnLine.run(state); RETURNER PASSED!!!");

		return;
	}
}
