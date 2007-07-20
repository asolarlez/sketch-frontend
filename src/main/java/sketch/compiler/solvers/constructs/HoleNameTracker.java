/**
 * 
 */
package streamit.frontend.tosbit;

import streamit.frontend.nodes.ExprFunCall;
import streamit.frontend.nodes.StmtFor;
import streamit.frontend.nodes.StmtLoop;

/**
 * @author asolar
 *
 */
public interface HoleNameTracker {	
	String getName(Object hole);
	void reset();
	void pushFunCall(ExprFunCall call);
	void pushLoop(StmtLoop loop);	
	void pushFor(StmtFor floop);
	void regLoopIter();
	
}
