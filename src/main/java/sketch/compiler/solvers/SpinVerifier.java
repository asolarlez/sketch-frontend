/**
 *
 */
package streamit.frontend.solvers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

	/** Parses the line number at which a particular thread is blocked. */
	protected static final String BLOCK_REGEX =
		"^\\s*\\d+:\\s*proc\\s+(\\d+) .* line (\\d+) .*\\(invalid end state\\)$";

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

	/** Parses a single statement in a counterexample trace. */
	protected static final String STEP_REGEX =
		"^\\s*\\d+:\\s*proc\\s+(\\d+).* line (\\d+) [^\\[]*\\[(.*)\\]$";

	public CEtrace parseTrace (String trace) {
		CEtrace cex = new CEtrace ();
		Matcher m = Pattern.compile (STEP_REGEX, Pattern.MULTILINE).matcher (trace);

		while (m.find ()) {
			int thread = Integer.parseInt (m.group (1));
			int line = Integer.parseInt (m.group (2));
			String stmt = m.group (3);

			log ("  parsed step:  thread "+ thread +", line "+ line +", stmt \""+ stmt +"\"");

			if (stmt.startsWith ("_ = "))
				cex.addStep (thread, Integer.parseInt (stmt.substring (4)));
			else {	// Might be a conditional atomic
				Integer stmtNum = lineToStmtnum.get (line);
				if (null != stmtNum) {
					cex.addStep (thread, stmtNum);
					log ("    (conditional atomic step)");
				}
			}
		}

		assert cex.steps.size () > 0 : "Uh-oh!  No steps in counterexample";

		return cex;
	}

/*
	protected void addSpinStats (SpinSolutionStatistics s, String trace) {
		List<String> res;

		res = search (trace, "pan: elapsed time (\\d+(?:\\.\\d+)?)");
		if (null != res)
			s.spinTimeMs = (long) (1000.0 * Float.parseFloat (res.get (0)));

		res = search (trace, "(\\d+) states, stored");
		if (null != res)  s.spinNumStates = Long.parseLong (res.get (0));

		res = search (trace,
				"(\\d+\\.\\d+)\\s+equivalent memory usage.*$.*"+
				"(\\d+\\.\\d+)\\s+actual memory usage.*$.*");
		if (null != res) {
			s.spinEquivStateMemBytes = (long) (1048576.0 * Float.parseFloat (res.get (0)));
			s.spinActualStateMemBytes = (long) (1048576.0 * Float.parseFloat (res.get (1)));

			//res =
		}


		/*
		=====  Full Version  =====
		---------------------------
		(Spin Version 5.1.4 -- 27 January 2008)
		Warning: Search not completed
			+ Partial Order Reduction
			+ Compression

		Full statespace search for:
			never claim         	- (none specified)
			assertion violations	+
			cycle checks       	- (disabled by -DSAFETY)
			invalid end states	+

		State-vector 9772 byte, depth reached 488, errors: 1
		      471 states, stored
		        0 states, matched
		      471 transitions (= stored+matched)
		       18 atomic steps
		hash conflicts:         0 (resolved)

		Stats on memory usage (in Megabytes):
		    4.397	equivalent memory usage for states (stored*(State-vector + overhead))
		    3.111	actual memory usage for states (compression: 70.76%)
		         	state-vector as stored = 6910 byte + 16 byte overhead
		    2.000	memory used for hash table (-w19)
		    0.305	memory used for DFS stack (-m10000)
		    5.125	total actual memory usage
		nr of templates: [ globals chans procs ]
		collapse counts: [ 23 393 80 ]

		pan: elapsed time 0.02 seconds
		pan: rate     23550 states/second


		===== Lite =====

		(Spin Version 5.1.4 -- 27 January 2008)
		Warning: Search not completed
			+ Partial Order Reduction
			+ Compression

		Full statespace search for:
			never claim         	- (none specified)
			assertion violations	+
			cycle checks       	- (disabled by -DSAFETY)
			invalid end states	+

		State-vector 2488 byte, depth reached 423, errors: 1
		      335 states, stored
		        0 states, matched
		      335 transitions (= stored+matched)
		       89 atomic steps
		hash conflicts:         0 (resolved)

		    5.125	memory usage (Mbyte)

		nr of templates: [ globals chans procs ]
		collapse counts: [ 22 266 70 ]

		pan: elapsed time 0.01 seconds
	}
*/

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

	/** Returns null if the pattern wasn't found, otherwise returns a list
	 * of the matched groups. */
	protected List<String> search (String S, String regex) {
		Matcher m = Pattern.compile (regex, Pattern.MULTILINE).matcher (S);
		List<String> groups = null;

		if (m.find ()) {
			groups = new ArrayList<String> ();
			for (int i = 1; i <= m.groupCount (); ++i)
				groups.add (m.group (i));
		}

		return groups;
	}
}
