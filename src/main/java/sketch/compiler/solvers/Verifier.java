package streamit.frontend.solvers;

import streamit.frontend.tosbit.AbstractValueOracle;

public interface Verifier {

	public CounterExample verify(AbstractValueOracle oracle);
	public SolutionStatistics getLastSolutionStats ();
}
