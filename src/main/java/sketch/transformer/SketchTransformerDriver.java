package sketch.transformer;

import sketch.compiler.ast.core.Program;

public class SketchTransformerDriver {
	CodeBlock code_block;

	public SketchTransformerDriver(CodeBlock _code_block) {
		code_block = _code_block;
	}

	public Program eval(Program program) {
		State state = new State(program);

		code_block.run(state);

		return state.get_return();
	}
}
