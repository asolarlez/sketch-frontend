/**
 *
 */
package streamit.frontend.solvers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import streamit.frontend.nodes.Program;
import streamit.frontend.nodes.StmtAtomicBlock;
import streamit.frontend.nodes.TempVarGen;
import streamit.frontend.passes.LowerLoopsToWhileLoops;
import streamit.frontend.spin.Configuration;
import streamit.frontend.spin.Executer;
import streamit.frontend.spin.Preprocessor;
import streamit.frontend.spin.PromelaCodePrinter;
import streamit.frontend.stencilSK.EliminateStarStatic;
import streamit.frontend.tosbit.ValueOracle;
import streamit.misc.NullStream;

/**
 * @author Chris Jones
 */
public class SpinVerifier implements Verifier {
	public static final int VECTORSZ_GUESS = 2048;

	protected Program prog;
	protected Configuration config;
	protected TempVarGen varGen;
	protected boolean debug, cleanup;
	protected int vectorSize;	// bytes
	protected Map<Integer, Integer> lineToStmtnum;

	/** Parses a single statement in a counterexample trace. */
	protected static final String STEP_REGEX =
		//"^\\s*\\d+:\\s*proc\\s+(\\d+)[^\\[]+\\[_ = (\\d+)\\]$";
		"^\\s*\\d+:\\s*proc\\s+(\\d+).* line (\\d+) .*\\[([^\\]]+)\\]$";

	/** Parses the line number at which a particular thread is blocked. */
	protected static final String BLOCK_REGEX =
		"^\\s*\\d+:\\s*proc\\s+(\\d+) .* line (\\d+) .*\\(invalid end state\\)$";

	public SpinVerifier (TempVarGen v, Program p) {
		this (v, p, new Configuration (), false, true, VECTORSZ_GUESS);
	}
	public SpinVerifier (TempVarGen v, Program p, Configuration c,
			boolean _debug, boolean _cleanup, int initVectorSize) {
		varGen = v;
		prog = preprocess (p, varGen);
		config = c;
		debug = _debug;
		cleanup = _cleanup;
		vectorSize = initVectorSize;

		// TODO: this should be moved to 'spinify()' if 'spinify()' starts
		// doing optimizations that might throw off this mapping.
		lineToStmtnum = buildLineToStmtnumMap (prog);

		config.vectorSizeBytes (vectorSize);
	}

	public CounterExample verify(ValueOracle oracle) {
		while (true) {
			Executer spin = Executer.makeExecuter (spinify (oracle), config, debug, cleanup);
			try { spin.run (); } catch (IOException ioe) {
				throw new RuntimeException ("Fatal error invoking spin", ioe);
			}
			String out = spin.getOutput ();
			String trail = spin.getTrail ();

			if (vectorSizeTooSmall (out)) {
				vectorSize *= 2;
				config.vectorSizeBytes (vectorSize);
				log ("VECTORSZ too small, increased to "+ vectorSize);
			} else if (deadlock (out)) {
				CounterExample cex = parseDeadlockTrace (trail);
				log ("counterexample from deadlock: "+ cex);
				return cex;
			} else if (trail.length () == 0) {
				return null;	// success!
			} else {
				CounterExample cex = parseTrace (trail);
				log ("counterexample: "+ cex);
				return cex;
			}
		}
	}

	/**
	 * Insert the hole values in HOLEVALS into PROG, and return the
	 * program resulting from this and other preprocessing steps.
	 */
	protected Program spinify (ValueOracle holeVals) {
		// TODO: might need other passes to make SPIN verification more
		// efficient
		return (Program) prog.accept (new EliminateStarStatic (holeVals));
	}

	/**
	 * Return true iff SPIN tells us that the vector size was too small to
	 * complete verification.
	 */
	protected boolean vectorSizeTooSmall (String out) {
		return 0 <= out.indexOf ("VECTORSZ too small");
	}

	protected boolean deadlock (String out) {
		return 0 <= out.indexOf ("pan: invalid end state");
	}

	public CEDeadlockedTrace parseDeadlockTrace (String trace) {
		CEDeadlockedTrace cex =	new CEDeadlockedTrace (parseTrace (trace));
		Matcher m = Pattern.compile (BLOCK_REGEX, Pattern.MULTILINE).matcher (trace);

		while (m.find ()) {
			int thread = Integer.parseInt (m.group (1));
			int line = Integer.parseInt (m.group (2));
			Integer stmt = lineToStmtnum.get (line);
			if (null != stmt)
				cex.addBlockedStmt (thread, stmt);
		}

		assert cex.blocks.size () > 0 : "Uh-oh!  No threads were blocked";

		return cex;
	}

	public CEtrace parseTrace (String trace) {
		CEtrace cex = new CEtrace ();
		Matcher m = Pattern.compile (STEP_REGEX, Pattern.MULTILINE).matcher (trace);

		while (m.find ()) {
			int thread = Integer.parseInt (m.group (1));
			int line = Integer.parseInt (m.group (2));
			String stmt = m.group (3);

			if (stmt.startsWith ("_ = "))
				cex.addStep (thread, Integer.parseInt (stmt.substring (4)));
			else {	// Might be a conditional atomic
				Integer stmtNum = lineToStmtnum.get (line);
				if (null != stmtNum)
					cex.addStep (thread, stmtNum);
			}
		}

		assert cex.steps.size () > 0 : "Uh-oh!  No steps in counterexample";

		return cex;
	}

	protected Program preprocess (Program p, TempVarGen varGen) {
		p = (Program) p.accept(new Preprocessor(varGen));
		p = (Program) p.accept (new LowerLoopsToWhileLoops (varGen));
		return p;
	}

	/**
	 * This unholy little class overrides the Promela code printer's
	 * visitAtomicBlock() method to record the line numbers at which
	 * conditions of conditional atomics were seen.
	 */
	private static final class GetAtomicCondInfo extends PromelaCodePrinter {
		public Map<Integer, Integer> condMap = new HashMap<Integer, Integer> ();
		GetAtomicCondInfo () {  super (NullStream.INSTANCE);  }
		public Object visitStmtAtomicBlock (StmtAtomicBlock block) {
			if (block.isCond ())
				condMap.put (getLineNumber (), (Integer) block.getTag ());
			return super.visitStmtAtomicBlock (block);
		}
	}
	protected Map<Integer, Integer> buildLineToStmtnumMap (Program p) {
		GetAtomicCondInfo info = new GetAtomicCondInfo ();
		p.accept (info);
		return info.condMap;
	}

	protected void log (String msg) {
		if (debug)  System.out.println ("[SPINVERIF][DEBUG] "+ msg);
	}
}
