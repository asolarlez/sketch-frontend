package streamit.frontend.experimental.deadCodeElimination;

import java.util.List;

import streamit.frontend.experimental.abstractValue;

public class joinAV extends abstractValue {
	int liveness;
	joinAV(int liveness){
		this.liveness = liveness;
	}
	
	@Override
	public abstractValue clone() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getIntVal() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public List<abstractValue> getVectValue() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasIntVal() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isBottom() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isVect() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void update(abstractValue v) {
		assert false;
	}

}
