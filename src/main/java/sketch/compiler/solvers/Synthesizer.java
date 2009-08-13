package sketch.compiler.solvers;

import sketch.compiler.solvers.constructs.AbstractValueOracle;

public interface Synthesizer {

	public void initialize();
	public void cleanup();
	public AbstractValueOracle nextCandidate(CounterExample couterExample);
	public SolutionStatistics getLastSolutionStats ();
}
