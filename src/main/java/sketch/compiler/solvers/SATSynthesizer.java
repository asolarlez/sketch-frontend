package streamit.frontend.solvers;

import streamit.frontend.CommandLineParamManager;
import streamit.frontend.nodes.TempVarGen;
import streamit.frontend.tosbit.ValueOracle;
import streamit.frontend.tosbit.recursionCtrl.RecursionControl;

public class SATSynthesizer extends SATBackend implements Synthesizer {

	SATSynthesizer(CommandLineParamManager params, RecursionControl rcontrol, TempVarGen varGen){
		super(params, rcontrol, varGen);
	}
	
	
	public ValueOracle nextCandidate(CounterExample couterExample) {
		// TODO Auto-generated method stub
		return null;
	}

}
