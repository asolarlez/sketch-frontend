package streamit.frontend.experimental.deadCodeElimination;

import streamit.frontend.experimental.abstractValue;
import streamit.frontend.experimental.abstractValueType;
import streamit.frontend.experimental.varState;
import streamit.frontend.nodes.Type;

public class LiveVariableVarState extends varState {
	
	LiveVariableVarState(Type t){
		super(t);
		init(newLHSvalue());
	}
	
	@Override
	public varState getDeltaClone(abstractValueType vt) {
		LiveVariableVarState vs = new LiveVariableVarState(getType());
		vs.helperDeltaClone(this, vt);
		return vs;
	}

	@Override
	public abstractValue newLHSvalue() {
		return new LiveVariableAV();
	}

	@Override
	public abstractValue newLHSvalue(int i) {
		assert false : "No arrays in this analysis";
		return null;
	}
	
	

}
