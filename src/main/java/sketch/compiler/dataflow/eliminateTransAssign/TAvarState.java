package streamit.frontend.experimental.eliminateTransAssign;

import java.util.Iterator;

import streamit.frontend.experimental.MethodState;
import streamit.frontend.experimental.abstractValue;
import streamit.frontend.experimental.abstractValueType;
import streamit.frontend.experimental.varState;
import streamit.frontend.nodes.Type;

public class TAvarState extends varState {
	final MethodState ms;
	final String name;
	TAvarState(String var, Type t, MethodState mstate){
		super(t);
		name = var;
		ms = mstate;
		init(newLHSvalue());
	}
	
	
	@Override
	public varState getDeltaClone(abstractValueType vt) {
		TAvarState st  = new TAvarState(name, getType(), ms);
		st.helperDeltaClone(this, vt);		
		return st;
	}

	@Override
	public abstractValue newLHSvalue() {
		return new transAssignAbsValue(name, ms);
	}

	@Override
	public abstractValue newLHSvalue(int i) {
		assert false : "No arrays in this analysis";
		return null;
	}
	
	public void outOfScope(){
		transAssignAbsValue av = (transAssignAbsValue)this.state(TAvalueType.vtype);
		Iterator<String> it = av.varsEqToMe.iterator();
		while(it.hasNext()){
			ms.setVarValue(ms.untransName(it.next()), new  taUpdater(taUpdater.CLEAR, av.me)  );			
		}
		if( av.varIamEqualTo != null  ){
			ms.setVarValue(ms.untransName(av.varIamEqualTo), new  taUpdater(taUpdater.REMOVE, av.me)  );
		}
	}
	
	public String toString(){
		if(this.state(TAvalueType.vtype) != null){
			return state(TAvalueType.vtype).toString();
		}
		return null;
	}

}
