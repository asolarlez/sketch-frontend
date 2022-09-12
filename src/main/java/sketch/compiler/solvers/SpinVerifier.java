/**
 *
 */
package sketch.compiler.solvers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.dataflow.eliminateTransAssign.EliminateTransAssns;
import sketch.compiler.main.cmdline.SketchOptions;
import sketch.compiler.main.par.ParallelSketchOptions;
import sketch.compiler.parallelEncoder.ParallelPreprocessor;
import sketch.compiler.passes.lowering.FlattenStmtBlocks;
import sketch.compiler.passes.lowering.HoistDeclarations;
import sketch.compiler.passes.lowering.LowerLoopsToWhileLoops;
import sketch.compiler.passes.lowering.MergeLocalStatements;
import sketch.compiler.passes.lowering.SeparateInitializers;
import sketch.compiler.passes.printers.SimpleCodePrinter;
import sketch.compiler.solvers.CEtrace.step;
import sketch.compiler.solvers.constructs.AbstractValueOracle;
import sketch.compiler.spin.Configuration;
import sketch.compiler.spin.EliminateDeadParallelCode;
import sketch.compiler.spin.Executer;
import sketch.compiler.spin.Preprocessor;
import sketch.compiler.spin.PromelaCodePrinter;
import sketch.compiler.stencilSK.EliminateHoleStatic;
import sketch.util.Misc;

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

	public CounterExample verify(AbstractValueOracle oracle) {
		while (true) {
			Executer spin = Executer.makeExecuter (
					spinify (oracle), config, reallyVerbose (), cleanup);
			try { spin.run ( SketchOptions.getSingleton().solverOpts.timeout ); } catch (IOException ioe) {
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

			CounterExample cex = extractTrace (out, trail);
			log (2, "Stats for last run:\n"+ lastSolveStats);
			return cex;
		}
	}

	public CounterExample extractTrace (String out, String trail) {
		if (trail.length () == 0) {
			lastSolveStats.success = true;
			return null;	// success!
		} else {
			CEtrace cex = parseTrace (trail);
			step lastStep;
			if (cex.steps.size () > 0)
				lastStep = cex.steps.get (cex.steps.size ()-1);
			else
				lastStep = new step (-1, -1);
			List<step> finalStates = parseFinalStates (trail, lastStep);
			cex.addSteps (finalStates);

			List<step> blocked = findBlockedThreads (trail);
			if (deadlock (out)) {
				assert blocked.size () > 0 : "Uh-oh!  No blocked threads";

				log (5, "  (counterexample from deadlock)");
				log (5, "  blocked threads: "+ blocked);
			}
			cex.addBlockedSteps (blocked);

			log (5, "  final states: "+ finalStates);
			log (5, "counterexample: "+ cex);

			return cex;
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
	protected Program spinify (AbstractValueOracle holeVals) {
		
		Program p = (Program) prog.accept (new EliminateHoleStatic (holeVals));

		log ("Cleaning up the next candidate.");
		if (reallyREALLYVerbose ()) {
			log ("Before specialization and optimization:");
			p.accept (new SimpleCodePrinter());
		}
        if (!ParallelSketchOptions.getSingleton().parOpts.playDumb) {
			//ToSBit.dump (p, "flatten");
			p = (Program) p.accept (new ParallelPreprocessor ());
			
			p = (Program) p.accept (new FlattenStmtBlocks ());
			//ToSBit.dump (p, "preproc");
            p = (Program) p.accept(new EliminateTransAssns(varGen));
			
            p = (Program) p.accept(new EliminateDeadParallelCode(varGen));
		
		//ToSBit.dump (p, "dead parallel");

		p = (Program) p.accept (new SeparateInitializers());
		{
			p = (Program) p.accept (new HoistDeclarations ());
			p = MergeLocalStatements.go (p);
			//ToSBit.dump (p, "merged local stmts (SpinVerif)");
		}
		
		}
		if (reallyREALLYVerbose ()) {
			log ("After specialization and optimization:");
			p.accept (new SimpleCodePrinter ());
		}

		p = (Program) p.accept(new Preprocessor (varGen));
		p = (Program) p.accept (new LowerLoopsToWhileLoops (varGen));
		//ToSBit.dump (p, "lower while loops");

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

	/** Parses the label of an conditional atomic block. */
	protected static final String ATOMIC_COND_REGEX =
		"\\(\\(\\("+ PromelaCodePrinter.atomicCondLbl +"==(\\d+)\\)";

	/** Parses thread ID and statement label of a blocked thread. */
	protected static final String BLOCK_REGEX =
		"^\\s*\\d+:\\s*proc\\s+(\\d+).*\\(invalid end state\\)"+
		"(?:\\r\\n|\\r|\\n)\\s+"+ ATOMIC_COND_REGEX;

	public List<step> findBlockedThreads (String trace) {
		List<step> blocked = new ArrayList<step> ();
		Matcher m = Pattern.compile (BLOCK_REGEX, Pattern.MULTILINE).matcher (trace);

		while (m.find ()) {
			int thread = Integer.parseInt (m.group (1));
			int stmt = Integer.parseInt (m.group (2));
			blocked.add (new step (thread, stmt));
		}

		return blocked;
	}

	/** Parses the label of a statement. */
	protected static final String STMT_LBL_REGEX =
		"_ = (\\d+)";

	/** Parses thread ID and statement label of a blocked thread. */
	protected static final String FINAL_STATE_REGEX =
		"^\\s*\\d+:\\s*proc\\s+(\\d+).*\\(invalid end state\\)"+
		"(?:\\r\\n|\\r|\\n)\\s+"+ STMT_LBL_REGEX;

	public List<step> parseFinalStates (String trace, step lastStep) {
		List<step> S = new ArrayList<step> ();
		Matcher m = Pattern.compile (FINAL_STATE_REGEX, Pattern.MULTILINE).matcher (trace);
		step first = null;

		while (m.find ()) {
			int thread = Integer.parseInt (m.group (1));
			int stmt = Integer.parseInt (m.group (2));

			if (thread == lastStep.thread)
				first = new step (thread, stmt);
			else
				S.add (new step (thread, stmt));
		}

		if (null != first)
			S.add (0, first);

		return S;
	}

	/** Parses a single statement in a counterexample trace. */
	protected static final String STEP_REGEX =
		"^\\s*\\d+:\\s*proc\\s+(\\d+)[^\\[]*\\[(.*)\\]$";

	public CEtrace parseTrace (String trace) {
		CEtrace cex = new CEtrace ();
		Matcher m = Pattern.compile (STEP_REGEX, Pattern.MULTILINE).matcher (trace);

		while (m.find ()) {
			int thread = Integer.parseInt (m.group (1));
			String stmt = m.group (2);

			log (5, "  parsed step:  thread "+ thread +", stmt \""+ stmt +"\"");

			if (stmt.startsWith ("_ = "))
				cex.addStep (thread, Integer.parseInt (stmt.substring (4)));
			else {	// Might be a conditional atomic
				List<String> e = Misc.search (stmt, ATOMIC_COND_REGEX);
				if (null != e) {
					cex.addStep (thread, Integer.parseInt (e.get (0)));
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

	protected boolean reallyVerbose () {  return verbosity >= 5;  }
	protected boolean reallyREALLYVerbose () {  return verbosity >= 7;  }

	protected void log (String msg) {  log (3, msg);  }
	protected void log (int minVerbosity, String msg) {
		if (verbosity >= minVerbosity)
			System.out.println ("[SPINVERIF]["+ minVerbosity+"] "+ msg);
	}
}
