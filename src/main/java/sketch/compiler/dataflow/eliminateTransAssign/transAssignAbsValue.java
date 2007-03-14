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
		final int ADD = taUpdater.ADD;
		final int REMOVE = taUpdater.REMOVE;
		final int CLEAR = taUpdater.CLEAR;
		final int OVERWRITE = taUpdater.OVERWRITE;
		if(v instanceof taUpdater){
			taUpdater tau = (taUpdater) v;
			if( tau.command == ADD){
				varsEqToMe.add(tau.arg);
			}
			if( tau.command == REMOVE){
				assert varsEqToMe.contains(tau.arg) : "This is an invariant";
				varsEqToMe.remove(tau.arg);
			}
			if( tau.command == CLEAR ){
				assert varIamEqualTo == tau.arg: "This is an invariant";
				varIamEqualTo = null;
			}
			if( tau.command == OVERWRITE ){
				assert tau.tav.me == me : "You can only overwrite yourself";
				/*
				if(this.varIamEqualTo != tau.tav.varIamEqualTo){
					if(varIamEqualTo != null ){
						ms.setVarValue(ms.untransName(varIamEqualTo), new  taUpdater(REMOVE, me)  );
					}
					this.varIamEqualTo = tau.tav.varIamEqualTo;
					ms.setVarValue(ms.untransName( tau.tav.varIamEqualTo ), new  taUpdater(ADD, me) );
				}
				
				Iterator<String> it = varsEqToMe.iterator();
				while(it.hasNext()){
					String newNm = it.next();
					if( !tau.tav.varsEqToMe.contains(newNm) ){
						ms.setVarValue(ms.untransName(newNm), new  taUpdater(CLEAR, me)  );
					}
				}
				this.varsEqToMe.clear();
				*/
				this.varIamEqualTo = tau.tav.varIamEqualTo;
				this.varsEqToMe = tau.tav.varsEqToMe;
			}
		}else{
			if(v instanceof transAssignAbsValue ){
				transAssignAbsValue ta = (transAssignAbsValue) v;
				if(varIamEqualTo != null ){
					ms.setVarValue(ms.untransName(varIamEqualTo), new  taUpdater(REMOVE, me)  );
				}
				varIamEqualTo = ta.me;
				ms.setVarValue(ms.untransName( ta.me ), new  taUpdater(ADD, me) );			
			}
			if(v instanceof TAsupperBottom  ){
				if(varIamEqualTo != null ){
					ms.setVarValue(ms.untransName(varIamEqualTo), new  taUpdater(REMOVE, me)  );
				}
				varIamEqualTo = null;
			}
			Iterator<String> it = varsEqToMe.iterator();
			while(it.hasNext()){
				ms.setVarValue(ms.untransName(it.next()), new  taUpdater(CLEAR, me)  );				
			}
			varsEqToMe.clear();
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




