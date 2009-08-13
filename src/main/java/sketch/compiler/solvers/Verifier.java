package sketch.compiler.solvers;

import sketch.compiler.solvers.constructs.AbstractValueOracle;

public interface Verifier {

	public CounterExample verify(AbstractValueOracle oracle);
	public SolutionStatistics getLastSolutionStats ();
}
