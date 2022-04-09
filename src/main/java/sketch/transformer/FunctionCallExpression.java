package sketch.transformer;

public class FunctionCallExpression extends Expression {
	FunctionCall function_call;

	public FunctionCallExpression(FunctionCall _function_call) {
		function_call = _function_call;
	}

	public String toString() {
		return function_call.toString();
	}
}
