/**
 * 
 */
package sketch.compiler.solvers.constructs;

import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.stmts.StmtFor;
import sketch.compiler.ast.core.stmts.StmtLoop;

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
	public boolean allowMemoization();	
}
