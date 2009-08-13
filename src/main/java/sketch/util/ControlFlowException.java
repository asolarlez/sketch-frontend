/**
 *
 */
package sketch.util;

/**
 * A bastard exception that represents not an error, but a desire to bypass
 * the normal control flow.
 *
 * This can be used to, e.g., break out of a deep  'visitor'
 * traversal of an AST when some property of interest is found to hold true.
 *
 * @author <a href="mailto:cgjones@cs.berkeley.edu">Chris Jones</a>
 */
public class ControlFlowException extends RuntimeException {
	public static final long serialVersionUID = -1;		// not serializable

	public ControlFlowException (String tag) {
		super (tag);
	}

	public String getTag () {  return getMessage ();  }
}
