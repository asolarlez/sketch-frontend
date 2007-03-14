package streamit.frontend.experimental.deadCodeElimination;

import streamit.frontend.experimental.MethodState;
import streamit.frontend.experimental.abstractValue;
import streamit.frontend.experimental.abstractValueType;
import streamit.frontend.experimental.varState;
import streamit.frontend.nodes.Type;

public class LiveVariableVarState extends varState {
	final String name;
	final MethodState mstate;
	LiveVariableVarState(String name, Type t, MethodState mstate){
		super(t);		
		assert name != null;
		this.name = name;
		this.mstate = mstate;
		init(newLHSvalue());
	}
	
	@Override
	public varState getDeltaClone(abstractValueType vt) {
		LiveVariableVarState vs = new LiveVariableVarState(name, getType(), mstate);
		vs.helperDeltaClone(this, vt);
		return vs;
	}

	@Override
	public abstractValue newLHSvalue() {
		return new LiveVariableAV(name, mstate);
	}

	@Override
	public abstractValue newLHSvalue(int i) {
		assert false : "No arrays in this analysis";
		return null;
	}
	
	public void update(abstractValue idx, abstractValue val, abstractValueType vtype){
		abstractValue lhs = state(LiveVariableVType.vtype);
		if( val instanceof LVSet){
			if(idx  instanceof LiveVariableAV ){
			((LVSet)val).set.add((LiveVariableAV)idx);
			}else{
				((LVSet)val).set.addAll(((LVSet)idx).set);	
			}
			
		}else{
			assert val instanceof LiveVariableAV;
			LVSet tmp =  new LVSet();
			tmp.set.add((LiveVariableAV)val);
			if(idx  instanceof LiveVariableAV ){
				tmp.set.add((LiveVariableAV)idx);
			}else{
				tmp.set.addAll(((LVSet)idx).set);	
			}
			val = tmp;
		}
		lhs.update(val);
	}

}
