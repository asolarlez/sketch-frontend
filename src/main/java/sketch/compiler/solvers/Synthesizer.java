package streamit.frontend.solvers;

import streamit.frontend.tosbit.ValueOracle;

public interface Synthesizer {

	public ValueOracle nextCandidate(CounterExample couterExample);
	public SolutionStatistics getLastSolutionStats ();
}
