package streamit.frontend.experimental.deadCodeElimination;

import java.util.Iterator;
import java.util.List;

import streamit.frontend.experimental.abstractValue;

public class LiveVariableAV extends abstractValue {
	static final int DEAD=0;
	static final int LIVE=1;
	
	private int liveness;
	private boolean beenLive = false;
	
	
	public boolean hasBeenLive(){
		return beenLive;
	}
	
	LiveVariableAV(){
		setLiveness(DEAD);
	}
	
	
	LiveVariableAV(LiveVariableAV av){
		setLiveness(av.getLiveness());
	}
	
	
	@Override
	public abstractValue clone() {
		return new LiveVariableAV(this);
	}

	@Override
	public int getIntVal() {
		assert false : "No int val";
		return 0;
	}

	@Override
	public List<abstractValue> getVectValue() {
		assert false : "No List value";
		return null;
	}

	@Override
	public boolean hasIntVal() {		
		return false;
	}

	@Override
	public boolean isBottom() {
		return getLiveness() == DEAD;
	}

	@Override
	public boolean isVect() {
		return false;
	}

	@Override
	public void update(abstractValue v) {
		setLiveness(DEAD);
		if( v instanceof LiveVariableAV){
			LiveVariableAV lv = (LiveVariableAV) v;
			lv.setLiveness(LIVE);
		}
		if( v instanceof LVSet){
			((LVSet)v).enliven();
		}
		if( v instanceof joinAV){
			setLiveness(((joinAV) v).liveness);
		}
	}
	public String toString(){
		if(getLiveness() == LIVE ){
			return "LIVE";
		}
		return "DEAD";
	}


	/**
	 * @param liveness the liveness to set
	 */
	public void setLiveness(int liveness) {
		if(liveness == LIVE){ beenLive = true; }
		this.liveness = liveness;
	}


	/**
	 * @return the liveness
	 */
	public int getLiveness() {
		return liveness;
	}
	
	public boolean equals(Object obj){
		if(!(obj instanceof LiveVariableAV)) return false;
		return ((LiveVariableAV)obj).liveness == liveness;
	}

}
