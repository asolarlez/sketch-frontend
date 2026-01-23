package sketch.transformer;

public abstract class Expression extends Node {
	public abstract Param eval(State state);

	public abstract void run(State state);
}
