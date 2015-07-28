package sketch.compiler.dataflow.deadCodeElimination;

import java.util.List;
import java.util.Map;

import sketch.compiler.dataflow.abstractValue;

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

    @Override
    public Map<String, Map<String, abstractValue>> getADTcases() {
        // FIXME xzl should refine this
        return null;
    }

}
