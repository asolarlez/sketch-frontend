package streamit.frontend.solvers;

import streamit.frontend.tosbit.ValueOracle;

public interface Verifier {

	public CounterExample verify(ValueOracle oracle);
	
}
