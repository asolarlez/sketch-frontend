package sketch.compiler.dataflow.eliminateTransAssign;

import java.util.List;
import java.util.Map;

/**
 * 
 * This class is used to send commands to the transAssignAbsValue. 
 * Each code below corresponds to a command:
 * 
 * CLEAR: Sets 'varIamEqualTo' to null. If a variable had recorded that it was equal to 
 * some other var 'x', CLEAR tells it that it is no longer equal to 'x', either because 
 * it got modified, or because it went out of scope.
 * 
 * 
 * REMOVE: tells a transAssignAbsValue that arg should no longer be in the 'varsEqToMe' list, 
 * either because 'arg' changed, or because it went out of scope. 
 * 
 */

import sketch.compiler.dataflow.abstractValue;

public class taUpdater extends abstractValue {
	public static final int ADD = 0;
	public static final int REMOVE = 1;
	public static final int CLEAR = 2;
	public static final int OVERWRITE = 3;
	
	public int command;
	public String arg;
	public transAssignAbsValue tav;
	taUpdater(int command, String arg){
		this.command = command;
		this.arg = arg;
	}
	
	taUpdater(int command, transAssignAbsValue tav){
		assert command == OVERWRITE;
		this.command = command;
		this.tav = tav;
	}
	
	@Override
	public abstractValue clone() {
		assert false :" This shouldn't be called.";
		return null;
	}

	@Override
	public int getIntVal() {		
		return 0;
	}

	@Override
	public List<abstractValue> getVectValue() {
		assert false :" This shouldn't be called.";
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
		assert false :" This shouldn't be called.";
		// TODO Auto-generated method stub

	}


    @Override
    public Map<String, Map<String, abstractValue>> getADTcases() {
        // TODO xzl should we refine this?
        return null;
    }

}
