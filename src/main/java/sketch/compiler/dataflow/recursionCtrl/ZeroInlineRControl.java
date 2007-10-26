package streamit.frontend.tosbit.recursionCtrl;

import streamit.frontend.nodes.ExprFunCall;
import streamit.frontend.nodes.Function;
import streamit.frontend.nodes.Statement;

public class ZeroInlineRControl extends RecursionControl {

	
	@Override
	public boolean testBlock(Statement stmt) {
		return true;
	}
	
	@Override
	public void doneWithBlock(Statement stmt) {
		// nothing to do here.
	}

	@Override
	public int inlineLevel(ExprFunCall fun) {
		return 0; // no inlining so inline level is always zero.
	}

	@Override
	public boolean leaveCallsBehind() {
		return true; // that's the whole purpose of this class.
	}

	@Override
	public void popFunCall(ExprFunCall fun) {
		// Nothing to do here.
	}

	@Override
	public void pushFunCall(ExprFunCall fc, Function fun) {
		// Nothing to do here.
	}



	@Override
	public boolean testCall(ExprFunCall fc) {		
		return false; // don't inline any calls.
	}

}
