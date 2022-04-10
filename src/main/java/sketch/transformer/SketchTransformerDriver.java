package sketch.transformer;

import sketch.compiler.ast.core.Program;

public class SketchTransformerDriver {
	CodeBlock code_block;

	public SketchTransformerDriver(CodeBlock _code_block) {
		code_block = _code_block;
	}

	public void run(Program program) {
		State state = new State(program);

		code_block.run(state);
	}
}
