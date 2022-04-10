package sketch.transformer;

import java.util.Vector;

public class CodeBlock extends Node {
	Vector<UnitLine> code_block = new Vector<UnitLine>();

	public void push_back(UnitLine expressionLine) {
		code_block.add(expressionLine);
	}

	public boolean isEmpty() {
		return code_block.isEmpty();
	}

	public int size() {
		return code_block.size();
	}

	public void run(State state) {
		for (int i = 0; i < code_block.size(); i++) {
			code_block.get(i).run(state);
		}
	}
}
