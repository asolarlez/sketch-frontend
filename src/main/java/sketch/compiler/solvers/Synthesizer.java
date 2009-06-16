package streamit.frontend.solvers;

import streamit.frontend.tosbit.AbstractValueOracle;

public interface Synthesizer {

	public void initialize();
	public void cleanup();
	public AbstractValueOracle nextCandidate(CounterExample couterExample);
	public SolutionStatistics getLastSolutionStats ();
}
