package sketch.transformer;

import java.util.Map;
import java.util.TreeMap;

import sketch.compiler.ast.core.Program;

public class State {
	Map<String, Param> state = new TreeMap<String, Param>();
	private Program program;

	public Program get_program() {
		return program;
	}

	public State(Program _program) {
		program = _program;
	}

	public void add(Identifier identifier, Param param)
	{
		String key = identifier.toString();
		assert (!state.containsKey(key));
		state.put(key, param);
	}
}
