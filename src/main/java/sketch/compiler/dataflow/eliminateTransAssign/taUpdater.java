package streamit.frontend.experimental.eliminateTransAssign;

import java.util.List;

import streamit.frontend.experimental.abstractValue;

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

}
