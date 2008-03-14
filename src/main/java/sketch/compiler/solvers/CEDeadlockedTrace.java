/**
 *
 */
package streamit.frontend.solvers;

import java.util.ArrayList;
import java.util.List;

/**
 * A schedule that resulted in a deadlock.
 *
 * This counterexample trace extends the CEtrace with the statements at which
 * each thread was blocked, if any.
 *
 * NOTE: threads without entries in the 'blocks' list are assumed to have
 * finished normally, or not contributed to the deadlock condition.
 *
 * @author <a href="mailto:cgjones@cs.berkeley.edu">Chris Jones</a>
 */
public class CEDeadlockedTrace extends CEtrace {
	public List<step> blocks = new ArrayList<step> ();

	public CEDeadlockedTrace (CEtrace cex) {
		steps = new ArrayList<step> (cex.steps);
	}

	public boolean deadlocked () { return true; }

	public void addBlockedStmt (int thread, int stmt) {
		blocks.add (new step (thread, stmt));
	}

	public String toString () {
		return super.toString () +"[Blocked: "+ blocks +"]";
	}
}
