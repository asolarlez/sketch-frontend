package streamit.frontend.solvers;

import streamit.frontend.tosbit.AbstractValueOracle;

public interface Synthesizer {

	public AbstractValueOracle nextCandidate(CounterExample couterExample);
	public SolutionStatistics getLastSolutionStats ();
}
