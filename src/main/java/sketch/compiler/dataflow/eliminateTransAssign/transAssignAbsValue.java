package streamit.frontend.experimental.eliminateTransAssign;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import streamit.frontend.experimental.MethodState;
import streamit.frontend.experimental.abstractValue;
import streamit.frontend.experimental.varState;


public class transAssignAbsValue extends abstractValue {
	String varIamEqualTo=null;
	Set<String> varsEqToMe;
	final String me;
	final MethodState ms;
	
	transAssignAbsValue(String name, MethodState ms){
		this.me = name;
		this.ms = ms;
		this.varsEqToMe = new HashSet<String>();
	}
	
	
	transAssignAbsValue(transAssignAbsValue ta){
		varIamEqualTo = ta.varIamEqualTo;
		varsEqToMe = new HashSet<String>(ta.varsEqToMe);
		me = ta.me;
		ms = ta.ms;
	}
	
	
	@Override
	public abstractValue clone() {
		// TODO Auto-generated method stub
		transAssignAbsValue ta = new transAssignAbsValue(this);
		
		return ta;
	}

	@Override
	public int getIntVal() {
		assert false : "This class won't return int vals.";
		return 0;
	}

	@Override
	public List<abstractValue> getVectValue() {
		assert false : "This class won't return list vals for now.";
		return null;
	}

	@Override
	public boolean hasIntVal() {
		return false;
	}

	@Override
	public boolean isBottom() {
		return varIamEqualTo==null && varsEqToMe.size() == 0;
	}

	@Override
	public boolean isVect() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void update(abstractValue v) {
		if(v instanceof transAssignAbsValue ){
			transAssignAbsValue ta = (transAssignAbsValue) v; 			
			
			
			
			if(ta.me.equals(me)){
				this.varIamEqualTo = ta.varIamEqualTo;
				this.varsEqToMe = ta.varsEqToMe;
			}else{
				varIamEqualTo = ta.me;
				ta.varsEqToMe.add(me);
			}
		}
		if(v instanceof TAsupperBottom  ){
			if(varIamEqualTo != null ){
				varState vs = ms.UTvarState(varIamEqualTo);
				abstractValue tmp = vs.state(TAvalueType.vtype);
				transAssignAbsValue tatmp = (transAssignAbsValue)tmp;
				assert tatmp.varsEqToMe.contains(me) : "This is an invariant";
				tatmp.varsEqToMe.remove(me);
			}
			varIamEqualTo = null;
		}
		Iterator<String> it = varsEqToMe.iterator();
		while(it.hasNext()){
			varState vs = ms.UTvarState(it.next());
			abstractValue tmp = vs.state(TAvalueType.vtype);
			transAssignAbsValue tatmp = (transAssignAbsValue)tmp;
			assert tatmp.varIamEqualTo == me : "This is an invariant";
			tatmp.varIamEqualTo = null;
		}
	}
	public String toString(){		
		return "["+me+", "  +  varIamEqualTo + ", " + varsEqToMe + "]";
	}
	public boolean equals(Object obj){
		if(!(obj instanceof transAssignAbsValue)) return false;
		transAssignAbsValue ta = (transAssignAbsValue) obj;
		if(!ta.me.equals(me)) return false;
		if( ta.varIamEqualTo != null  ){
			if(!ta.varIamEqualTo.equals(varIamEqualTo)) return false;
		}else{
			if( varIamEqualTo != null ) return false;
		}
		if(varsEqToMe.size() != ta.varsEqToMe.size()) return false;
		Iterator<String> it = varsEqToMe.iterator();
		while(it.hasNext()){
			if(!ta.varsEqToMe.contains(it.next())) return false; 
		}
		return true;
	}
}




