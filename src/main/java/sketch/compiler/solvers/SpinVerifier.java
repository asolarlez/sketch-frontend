/**
 *
 */
package streamit.frontend.solvers;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import streamit.frontend.nodes.Program;
import streamit.frontend.solvers.CEtrace.step;
import streamit.frontend.tosbit.ValueOracle;
import streamit.frontend.tospin.SpinExecuter;

/**
 * @author Chris Jones
 */
public class SpinVerifier implements Verifier {
	protected Program prog;
	protected boolean debug, cleanup;
	protected static final String STEP_REGEX =
		"^\\s*\\d+:\\s*proc\\s+(\\d+)[^\\[]+\\[_ = (\\d+)\\]$";

	public SpinVerifier (Program p) { this (p, false, true); }
	public SpinVerifier (Program p, boolean _debug, boolean _cleanup) {
		prog = p;
		debug = _debug;
		cleanup = _cleanup;
	}

	public CounterExample verify(ValueOracle oracle) {
		//SpinExecuter spin = SpinExecuter.makeExecuter (prog, oracle);
		SpinExecuter spin = SpinExecuter.makeExecuter (prog, oracle, debug, cleanup);
		try { spin.run (); } catch (IOException ioe) {
			throw new RuntimeException ("Fatal error invoking spin", ioe);
		}
		String trail = spin.getTrail ();
		return (trail.length () > 0) ? parseTrace (trail) : null;
	}

	public CounterExample parseTrace (String trace) {
		CEtrace cex = new CEtrace ();
		Matcher m = Pattern.compile (STEP_REGEX).matcher (trace);

		while (m.find ()) {
			int thread = Integer.parseInt (m.group (1));
			int stmt = Integer.parseInt (m.group (2));
			cex.steps.add (new step (thread, stmt));
		}

		return cex;
	}
}
