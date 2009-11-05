package sketch.compiler.smt.partialeval;

public class AssertionFailedException extends RuntimeException {

	public AssertionFailedException(String message) {
		super(message);
	}

	public AssertionFailedException(Throwable cause) {
		super(cause);
	}

	public AssertionFailedException(String message, Throwable cause) {
		super(message, cause);
	}

}
