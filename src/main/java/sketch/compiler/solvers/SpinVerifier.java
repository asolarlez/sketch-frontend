/**
 *
 */
package streamit.frontend.solvers;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import streamit.frontend.nodes.Program;
import streamit.frontend.nodes.TempVarGen;
import streamit.frontend.passes.LowerLoopsToWhileLoops;
import streamit.frontend.spin.Configuration;
import streamit.frontend.spin.Executer;
import streamit.frontend.spin.Preprocessor;
import streamit.frontend.stencilSK.EliminateStarStatic;
import streamit.frontend.tosbit.ValueOracle;

/**
 * @author Chris Jones
 */
public class SpinVerifier implements Verifier {
	protected Program prog;
	protected Configuration config;
	protected TempVarGen varGen;
	protected boolean debug, cleanup;
	protected static final String STEP_REGEX =
		"^\\s*\\d+:\\s*proc\\s+(\\d+)[^\\[]+\\[_ = (\\d+)\\]$";

	protected int vectorSize = 2048;	// bytes

	public SpinVerifier (TempVarGen v, Program p) {
		this (v, p, new Configuration (), false, true);
	}
	public SpinVerifier (TempVarGen v, Program p, Configuration c,
			boolean _debug, boolean _cleanup) {
		varGen = v;
		prog = p;
		config = c;
		debug = _debug;
		cleanup = _cleanup;

		config.vectorSizeBytes (vectorSize);
	}

	public CounterExample verify(ValueOracle oracle) {
		while (true) {
			//SpinExecuter spin = SpinExecuter.makeExecuter (prog, oracle);
			Executer spin = Executer.makeExecuter (spinify (oracle), config, debug, cleanup);
			try { spin.run (); } catch (IOException ioe) {
				throw new RuntimeException ("Fatal error invoking spin", ioe);
			}

			String trail = spin.getTrail ();
			if (vectorSizeTooSmall (trail)) {
				vectorSize *= 2;
				config.vectorSizeBytes (vectorSize);
				log ("VECTORSZ too small, increased to "+ vectorSize);
			} else if (trail.length () == 0) {
				return null;	// success!
			} else {
				return parseTrace (trail);
			}
		}
	}

	public CounterExample parseTrace (String trace) {
		CEtrace cex = new CEtrace ();
		Matcher m = Pattern.compile (STEP_REGEX, Pattern.MULTILINE).matcher (trace);

		while (m.find ()) {
			int thread = Integer.parseInt (m.group (1));
			int stmt = Integer.parseInt (m.group (2));
			cex.addStep (thread, stmt);
		}

		assert cex.steps.size () > 0 : "Uh-oh!  No steps in counterexample";

		return cex;
	}

	/**
	 * Insert the hole values in HOLEVALS into PROG, and return the
	 * program resulting from this and other preprocessing steps.
	 */
	protected Program spinify (ValueOracle holeVals) {
		Program p = (Program) prog.accept(new Preprocessor(varGen));

		p = (Program) p.accept (new EliminateStarStatic (holeVals));
		p = (Program) p.accept (new LowerLoopsToWhileLoops (varGen));
		// TODO: might need other passes to make SPIN verification more
		// efficient

		return p;
	}

	/**
	 * Return true iff SPIN tells us that the vector size was too small to
	 * complete verification.
	 */
	protected boolean vectorSizeTooSmall (String out) {
		return 0 <= out.indexOf ("VECTORSZ too small");
	}

	protected void log (String msg) {
		if (debug)  System.out.println ("[SPINVERIF][DEBUG] "+ msg);
	}
}
