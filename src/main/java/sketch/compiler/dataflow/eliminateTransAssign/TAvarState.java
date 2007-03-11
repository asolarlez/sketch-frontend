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
			varState vs = ms.UTvarState(it.next());
			abstractValue tmp = vs.state(TAvalueType.vtype);
			transAssignAbsValue tatmp = (transAssignAbsValue)tmp;
			assert tatmp.varIamEqualTo == av.me : "This is an invariant";
			tatmp.varIamEqualTo = null;
		}
		if( av.varIamEqualTo != null  ){
			varState vs = ms.UTvarState(av.varIamEqualTo);
			abstractValue tmp = vs.state(TAvalueType.vtype);
			transAssignAbsValue tatmp = (transAssignAbsValue)tmp;
			assert tatmp.varsEqToMe.contains(av.me) : "This is an invariant: "+ name + "  " + tatmp.me;
			tatmp.varsEqToMe.remove(av.me);
		}
		
		System.out.println(" OUT OF SCOPE " + this.name);
		
	}
	
	public String toString(){
		if(this.state(TAvalueType.vtype) != null){
			return state(TAvalueType.vtype).toString();
		}
		return null;
	}

}
