package streamit.frontend.tosbit.recursionCtrl;
import streamit.frontend.nodes.ExprFunCall;
import streamit.frontend.nodes.Function;
import streamit.frontend.nodes.Statement;

/**
 * 
 * One problem with inlining functions is that the size of the resulting code 
 * can grow exponentially. To avoid this, we want to use some heuristics to 
 * decide when to inline a function call and when not. The purpose of 
 * RecursionControl is to administer these heuristics. 
 * 
 * The recursion control class is informed every time the 
 * partial evaluator enters or leaves a statement block, or a function call, 
 * and based on this information, it decides whether a particular 
 * function call should be inlined.
 * 
 * Since the partial evaluator is supposed to produce function-free code, 
 * it is assumed that function calls not inlined will be replaced with 
 * <code>assert false</code>. Thus, a basic block containing any such functions might 
 * as well be replaced entirely with <code>assert false</code>. Thus, the 
 * RecursionControl also allows one to test for basic blocks that 
 * will be left with un-inlined functions. 
 * 
 * Functions should be called in the following matching pairs:
 * 
 * 
 *  pushFunCall(x) ... popFunCall(x)
 *  (t = testBlock(stmt)) ... doneWithBlock(stmt)

 * 
 * @author asolar
 *
 */

public abstract class RecursionControl{
	/**
	 * The function that is actually used for inlining decisions may be different from the
	 * actual function called because calls to sketches are replaced by calls to their respective specs.
	 * @param fc The actual function call as it appears on the program.
	 * @param fun The function that was actually used for inlining decisions.
	 */
	public abstract void pushFunCall(ExprFunCall fc, Function fun);
	public abstract void popFunCall(ExprFunCall fun);
	/**
	 * 
	 * @param fun
	 * @return the number of levels of inlining for a particular function.
	 */
	public abstract int inlineLevel(ExprFunCall fun);
	/**
	 * @note Calls to testBlock should alwayws be paired with calls to doneWithBlock.
	 * @param stmt
	 * @return false if the block has any call that will not be inlined.
	 */
	public abstract boolean testBlock(Statement stmt);
	public abstract void doneWithBlock(Statement stmt);
	/**
	 * 
	 * @param fc
	 * @return return true if this call should be inlined.
	 */
	public abstract boolean testCall(ExprFunCall fc);
	/**
	 * This function tells the caller whether the recursion control wishes to leave un-inlined functions behind as function calls or whether
	 * they should be replaced by assertions.
	 * 
	 * @return
	 */
	public abstract boolean leaveCallsBehind();
	
	public String debugMsg(){
		return "";
	}
	
}
