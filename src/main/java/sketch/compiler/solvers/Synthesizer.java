package streamit.frontend.solvers;

import streamit.frontend.nodes.Program;
import streamit.frontend.tosbit.ValueOracle;

public abstract class Synthesizer {
	
	public abstract ValueOracle nextCandidate(Program couterExample);
	
	

}
