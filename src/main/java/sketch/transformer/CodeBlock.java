package sketch.transformer;

import java.util.Vector;

public class CodeBlock extends Node {
	Vector<UnitLine> code_block = new Vector<UnitLine>();

	public void push_back(UnitLine expressionLine) {
		code_block.add(expressionLine);
	}
}
