package sketch.transformer;

public class FunctionCallExpression extends Expression {
	FunctionCall function_call;

	public FunctionCallExpression(FunctionCall _function_call) {
		function_call = _function_call;
	}

	public String toString() {
		return function_call.toString();
	}

	@Override
	public Param eval(State state) {
		return function_call.eval(state);
	}

	@Override
	public void run(State state) {
		function_call.run(state);
	}
}
