package sketch.transformer;

public class ObjectFunctionCall extends FunctionCall {
	Identifier object_identifier;

	public ObjectFunctionCall(Identifier object_identifier, FunctionCall _function_call) {
		super(_function_call);
		this.object_identifier = object_identifier;
	}

	public String toString() {
		return object_identifier.toString() + "." + super.toString();
	}
}
