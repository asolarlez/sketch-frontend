package streamit.frontend.tosbit.recursionCtrl;

import java.util.HashMap;

import streamit.frontend.nodes.ExprFunCall;
import streamit.frontend.nodes.Function;
import streamit.frontend.nodes.Statement;
import streamit.frontend.tosbit.recursionCtrl.BaseRControl.InlineCounter;

public class DelayedInlineRControl extends BaseRControl {
		
		
	public InlineCounter appearenceCounter = new InlineCounter();	
	private int MAX_OCC;
	
	
	public boolean leaveCallsBehind(){
		return true;
	}
	public DelayedInlineRControl(int maxInline, int maxOccurrence){
		super(maxInline);		
		MAX_OCC = maxOccurrence;
	}
	
	


	public void pushFunCall(ExprFunCall fc, Function fun) {
		icount.incInlineCounter(fc.getName());
		appearenceCounter.incInlineCounter(fc.getName());
	}
	

	public boolean testCall(ExprFunCall fc) {
		return false;
		//return super.testCall(fc) && appearenceCounter.getInlineCounter(fc.getName()) < MAX_OCC;
	}
	
	

}
