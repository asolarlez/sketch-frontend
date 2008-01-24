package streamit.frontend.solvers;

import streamit.frontend.tosbit.ValueOracle;

public abstract class Verifier {

	public abstract CounterExample verify(ValueOracle oracle);
	
}
