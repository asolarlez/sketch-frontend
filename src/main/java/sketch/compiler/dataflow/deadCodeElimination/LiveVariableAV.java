package streamit.frontend.experimental.deadCodeElimination;

import java.util.List;

import streamit.frontend.experimental.MethodState;
import streamit.frontend.experimental.abstractValue;

public class LiveVariableAV extends abstractValue {
	static final int DEAD=0;
	static final int LIVE=1;
	static final int HBLDEAD=2;
	final String name;
	private int liveness;
	private boolean beenLive = false;
	public final MethodState mstate;
	
	public boolean hasBeenLive(){
		return beenLive;
	}
	
	LiveVariableAV(String name, MethodState mstate){
		setLiveness(DEAD);
		assert name != null;
		this.name = name;
		this.mstate = mstate;
	}
	
	
	LiveVariableAV(LiveVariableAV av){
		setLiveness(av.getLiveness());
		this.name = av.name;
		this.mstate = av.mstate;
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
	public void makeVolatile(){
		super.makeVolatile();
		setLiveness(LIVE);
	}
	
	@Override
	public void update(abstractValue v) {
		if(!isVolatile){ setLiveness(DEAD); }
		if( v instanceof LiveVariableAV){
			LiveVariableAV lv = (LiveVariableAV) v;	
			if(lv.mstate != null  ){
				mstate.setVarValue(mstate.untransName(lv.name), new joinAV(LIVE));
			}
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
	private void setLiveness(int liveness) {
		if(liveness == LIVE || liveness == HBLDEAD){ beenLive = true; }
		this.liveness = liveness;
		if(liveness == LIVE){
			this.liveness =LIVE;
		}else{
			this.liveness = DEAD;			
		}		
	}


	/**
	 * @return the liveness
	 */
	public int getLiveness() {
		return liveness;
	}
	
	public boolean isLive () {
		return liveness == LIVE;
	}
	
	public boolean equals(Object obj){
		if(!(obj instanceof LiveVariableAV)) return false;
		return ((LiveVariableAV)obj).liveness == liveness;
	}

}
