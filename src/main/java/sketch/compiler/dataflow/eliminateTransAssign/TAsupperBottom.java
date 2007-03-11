package streamit.frontend.experimental.eliminateTransAssign;

import java.util.List;

import streamit.frontend.experimental.abstractValue;

public class TAsupperBottom extends abstractValue {

	@Override
	public abstractValue clone() {		
		return this;
	}

	@Override
	public int getIntVal() {
		assert false;
		return 0;
	}

	@Override
	public List<abstractValue> getVectValue() {
		assert false;
		return null;
	}

	@Override
	public boolean hasIntVal() {
		return false;
	}

	@Override
	public boolean isBottom() {
		return true;
	}

	@Override
	public boolean isVect() {
		return false;
	}

	@Override
	public void update(abstractValue v) {

	}

}
