package sketch.compiler.smt;

public class SolverFailedException extends Error {

	private static final long serialVersionUID = 6509191909628066999L;

	public SolverFailedException(String message) {
		super(message);
	}

	public SolverFailedException(Throwable cause) {
		super(cause);
	}

	public SolverFailedException(String message, Throwable cause) {
		super(message, cause);
	}

}
