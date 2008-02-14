/**
 *
 */
package streamit.frontend.tospin;

import streamit.frontend.nodes.Program;

/**
 * A class to verify a program using SPIN.
 *
 * TODO: refactor code that uses this class
 *
 * @deprecated	Use the streamit.solvers.SpinVerifier instead
 * @author Chris Jones
 */
public class SpinVerifier {
	protected streamit.frontend.solvers.SpinVerifier verif;

	public SpinVerifier (Program prog) {
		verif = new streamit.frontend.solvers.SpinVerifier (prog);
	}

	/**
	 * @return true iff all assertions in this.PROG are maintained for all
	 * possible executions.
	 */
	public boolean verify () { return verify (0); }

	public boolean verify (int timeoutMins) {
		return null == verif.verify (null);
	}

	public String getOutput () {
		return verif.getOutput ();
	}

	public static SpinVerifier makeVerifier (Program prog) {
		return new SpinVerifier (prog);
	}
}