package streamit.frontend.solvers;

import streamit.frontend.nodes.Program;
import streamit.frontend.tosbit.ValueOracle;

public interface Synthesizer {
	
	public ValueOracle nextCandidate(CounterExample couterExample);
	
	

}
