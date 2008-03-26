/**
 *
 */
package streamit.frontend.solvers;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import streamit.frontend.ToSBit;
import streamit.frontend.experimental.deadCodeElimination.EliminateDeadCode;
import streamit.frontend.experimental.eliminateTransAssign.EliminateTransAssns;
import streamit.frontend.experimental.preprocessor.PreprocessSketch;
import streamit.frontend.nodes.Program;
import streamit.frontend.nodes.StmtAtomicBlock;
import streamit.frontend.nodes.TempVarGen;
import streamit.frontend.passes.LowerLoopsToWhileLoops;
import streamit.frontend.spin.Configuration;
import streamit.frontend.spin.Executer;
import streamit.frontend.spin.Preprocessor;
import streamit.frontend.spin.PromelaCodePrinter;
import streamit.frontend.stencilSK.EliminateStarStatic;
import streamit.frontend.stencilSK.SimpleCodePrinter;
import streamit.frontend.tosbit.ValueOracle;
import streamit.misc.Misc;
import streamit.misc.NullStream;

/**
 * @author Chris Jones
 */
public class SpinVerifier implements Verifier {
	public static final int VECTORSZ_GUESS = 2048;

	protected Program prog;
	protected Configuration config;
	protected TempVarGen varGen;
	protected boolean cleanup;
	protected int verbosity;
	protected int vectorSize;	// bytes
	protected Map<Integer, Integer> lineToStmtnum;
	protected boolean preSimplify = false;

	protected SpinSolutionStatistics lastSolveStats;

	public void simplifyBeforeSolving(){
		preSimplify = true;
	}

	public SpinVerifier (TempVarGen v, Program p) {
		this (v, p, new Configuration (), 0, true, VECTORSZ_GUESS);
	}
	public SpinVerifier (TempVarGen v, Program p, Configuration c,
			int _verbosity, boolean _cleanup, int initVectorSize) {
		varGen = v;
		prog = p;
		config = c;
		verbosity = _verbosity;
		cleanup = _cleanup;
		vectorSize = initVectorSize;

		config.vectorSizeBytes (vectorSize);
	}

	public CounterExample verify(ValueOracle oracle) {
		while (true) {
			Executer spin = Executer.makeExecuter (
					spinify (oracle), config, reallyVerbose (), cleanup);
			try { spin.run (); } catch (IOException ioe) {
				throw new RuntimeException ("Fatal error invoking spin", ioe);
			}

			// XXX: here we assume that the amount of memory used by the
			// SPIN code generator and the C compiler are strictly less than
			// the amount used by the model checker, and therefore can safely
			// be ignored.
			lastSolveStats = new SpinSolutionStatistics ();
			lastSolveStats.cgenTimeMs = spin.getCodegenTimeMs ();
			lastSolveStats.compilerTimeMs = spin.getCompileTimeMs ();

			String out = spin.getOutput ();
			String trail = spin.getTrail ();

			if (vectorSizeTooSmall (out)) {
				vectorSize *= 2;
				config.vectorSizeBytes (vectorSize);
				printVectorszNotice ();
				continue;
			}

			addSpinStats (lastSolveStats, out);

			try {
				if (deadlock (out)) {
					CounterExample cex = parseDeadlockTrace (trail);
					log (5, "counterexample from deadlock: "+ cex);
					return cex;
				} else if (trail.length () == 0) {
					lastSolveStats.success = true;
					return null;	// success!
				} else {
					CounterExample cex = parseTrace (trail);
					log (5, "counterexample: "+ cex);
					return cex;
				}
			} finally {
				log (2, "Stats for last run:\n"+ lastSolveStats);
			}
		}
	}

	public SolutionStatistics getLastSolutionStats () {  return lastSolveStats;  }

	protected void printVectorszNotice () {
		log (3, "VECTORSZ too small, increased to "+ vectorSize);
		log (0,
"WARNING: The vector size guess was too small for this model.  Stats will be\n"+
"  inaccurate.  Next time, set the vectorszGuess to at least "+ vectorSize +".");
	}

	/**
	 * Insert the hole values in HOLEVALS into PROG, and return the
	 * program resulting from this and other preprocessing steps.
	 */
	protected Program spinify (ValueOracle holeVals) {
		Program p = (Program) prog.accept (new EliminateStarStatic (holeVals));

		if (preSimplify) {
			log ("Cleaning up the next candidate.");
			if (reallyREALLYVerbose ()) {
				log ("Before specialization and optimization:");
				p.accept (new SimpleCodePrinter());
			}
			prog = (Program) prog.accept (new PreprocessSketch (varGen, 0, ToSBit.visibleRControl (p)));
			prog = (Program) prog.accept (new EliminateTransAssns ());
			prog = (Program) prog.accept (new EliminateDeadCode (true));
			if (reallyREALLYVerbose ()) {
				log ("After specialization and optimization:");
				p.accept (new SimpleCodePrinter ());
			}
		}

		p = (Program) p.accept(new Preprocessor (varGen));
		p = (Program) p.accept (new LowerLoopsToWhileLoops (varGen));

		lineToStmtnum = buildLineToStmtnumMap (p);

		return p;
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

	//I don't quite understand the regex above, but it looks like it is filtering out
	//lines corresponding to thread zero. Is that true? : Armando to Chris.


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
		"^\\s*\\d+:\\s*proc\\s+(\\d+).* line ([\\-]?\\d+) [^\\[]*\\[(.*)\\]$";

	public CEtrace parseTrace (String trace) {
		CEtrace cex = new CEtrace ();
		Matcher m = Pattern.compile (STEP_REGEX, Pattern.MULTILINE).matcher (trace);

		while (m.find ()) {
			int thread = Integer.parseInt (m.group (1));
			int line = Integer.parseInt (m.group (2));
			String stmt = m.group (3);

			// TODO: this is a SPIN bug.  It at some point casts an int line
			// number to an short, which causes overflow here.
			if (line < 0)  line += 1 << 16;

			log (5, "  parsed step:  thread "+ thread +", line "+ line +", stmt \""+ stmt +"\"");

			if (stmt.startsWith ("_ = "))
				cex.addStep (thread, Integer.parseInt (stmt.substring (4)));
			else {	// Might be a conditional atomic
				Integer stmtNum = lineToStmtnum.get (line);
				if (null != stmtNum) {
					cex.addStep (thread, stmtNum);
					log (5, "    (conditional atomic step)");
				}
			}
		}

		//If the counterexample contains only the schedule for the parallel section,
		//the cex could have zero steps if the bug is found in the sequential section.
		//assert cex.steps.size () > 0 : "Uh-oh!  No steps in counterexample";

		return cex;
	}

	protected void addSpinStats (SpinSolutionStatistics s, String out) {
		List<String> res;

		res = Misc.search (out, "pan: elapsed time (\\d+(?:\\.\\d+)?)");
		assert null != res;
		s.spinTimeMs = (long) (1000.0 * Float.parseFloat (res.get (0)));

		res = Misc.search (out, "(\\d+) states, stored");
		assert null != res;
		s.spinNumStates = Long.parseLong (res.get (0));

		res = Misc.search (out,
				"(\\d+\\.\\d+)\\s+equivalent memory usage.*(?:\\r\\n|\\n|\\r).*"+
				"(\\d+\\.\\d+)\\s+actual memory usage for states");
		if (null != res) {
			// full state memory stats
			s.spinEquivStateMemBytes = (long) (1048576.0 * Float.parseFloat (res.get (0)));
			s.spinActualStateMemBytes = (long) (1048576.0 * Float.parseFloat (res.get (1)));
			s.spinStateCompressionPct = 100.0f *
				((float) s.spinActualStateMemBytes) / (float) s.spinEquivStateMemBytes;

			res = Misc.search (out, "(\\d+\\.\\d+)\\s+total actual memory usage");
			assert null != res;
			s.spinTotalMemBytes = (long) (1048576.0 * Float.parseFloat (res.get (0)));

			res = Misc.search (out, "pan: rate\\s+(\\d+(?:\\d+)?) states/second");
			if (null != res)
				s.spinStateExplorationRate =
					(long) Float.parseFloat (res.get (0));
		}
		else {
			// lite memory stats
			log (5, "SPIN only produced 'lite' memory stats");

			res = Misc.search (out, "(\\d+\\.\\d+)\\s+memory usage \\(Mbyte\\)");
			assert null != res;
			s.spinTotalMemBytes = (long) (1048576.0 * Float.parseFloat (res.get (0)));
		}
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
			if (block.isCond ()){
				Integer id = (Integer) block.getTag ();
				assert !condMap.containsKey (getLineNumber ()) : "Fix SPIN";
				condMap.put (getLineNumber (), id);
				Object o = super.visitStmtAtomicBlock (block);
				assert !condMap.containsKey (getLineNumber ()) : "Fix SPIN";
				condMap.put (getLineNumber (), id);
				return o;
			}
			return super.visitStmtAtomicBlock (block);
		}
	}
	protected Map<Integer, Integer> buildLineToStmtnumMap (Program p) {
		GetAtomicCondInfo info = new GetAtomicCondInfo ();
		p.accept (info);
		return info.condMap;
	}

	protected boolean reallyVerbose () {  return verbosity >= 5;  }
	protected boolean reallyREALLYVerbose () {  return verbosity >= 7;  }

	protected void log (String msg) {  log (3, msg);  }
	protected void log (int minVerbosity, String msg) {
		if (verbosity >= minVerbosity)
			System.out.println ("[SPINVERIF]["+ minVerbosity+"] "+ msg);
	}
}
