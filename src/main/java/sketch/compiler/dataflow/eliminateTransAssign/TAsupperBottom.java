package sketch.compiler.dataflow.eliminateTransAssign;

import java.util.List;
import java.util.Map;

import sketch.compiler.dataflow.abstractValue;

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

    @Override
    public Map<String, Map<String, abstractValue>> getADTcases() {
        // TODO xzl should we refine this?
        return null;
    }

}
