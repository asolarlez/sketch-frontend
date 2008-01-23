package streamit.frontend.solvers;

import streamit.frontend.nodes.Program;
import streamit.frontend.tosbit.ValueOracle;

public abstract class Verifier {

	public abstract Program verify(ValueOracle oracle);
	
}
