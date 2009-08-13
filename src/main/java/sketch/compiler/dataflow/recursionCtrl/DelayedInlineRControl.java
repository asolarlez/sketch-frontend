package sketch.compiler.dataflow.recursionCtrl;

import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.exprs.ExprFunCall;

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
