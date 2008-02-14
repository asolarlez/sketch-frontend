/**
 *
 */
package streamit.frontend.tospin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import streamit.frontend.ToSpin;
import streamit.frontend.nodes.Program;
import streamit.frontend.stencilSK.EliminateStarStatic;
import streamit.frontend.tosbit.ValueOracle;
import streamit.misc.Misc;
import streamit.misc.ProcessStatus;
import streamit.misc.SynchronousTimedProcess;

/**
 * @author Chris Jones
 */
public class SpinExecuter {
	protected Program		sourceProg;
	protected ValueOracle	holeVals;
	protected File			promelaCode;
	protected File			code;
	protected File			prog;
	protected File			trailFile;
	/* SPIN cannot be told where to put some output files, so we need this. */
	protected String		spinWorkDir;

	protected boolean		doCleanup;
	protected boolean		debug;

	protected int			exitCode;
	protected String		out;
	protected String		err;
	protected String		trail;

	public SpinExecuter (Program _prog, ValueOracle _holeVals,
			boolean _doCleanup, boolean _debug) {
		sourceProg = _prog;
		holeVals = _holeVals;
		doCleanup = _doCleanup;
		debug = _debug;
	}

	public void run () throws IOException {
		run (0);
	}

	public void run (int timeoutMins) throws IOException {
		try {
			init ();
			generatePromelaCode ();
			generateVerifierCode ();
			compileVerifier ();
			runVerifier (timeoutMins);
		} finally {
			if (doCleanup)
				deinit ();
		}
	}

	public String getOutput () {
		return out;
	}

	public String getErrorOutput () {
		return err;
	}

	public String getTrail () {
		return trail;
	}

	public boolean hasErrors () {
		return -1 == out.indexOf ("errors: 0");
	}

	protected void init () throws IOException {
		promelaCode = File.createTempFile ("promelaCode", ".pml");
		prog = File.createTempFile ("verifier", ".exe");
		spinWorkDir = System.getProperty ("java.io.tmpdir");
	}

	protected void deinit () {
		File[] toDelete = { promelaCode, prog, trailFile };
		for (File f : toDelete)
			if (null != f && f.exists ())
				f.delete ();

		String[] spinJunk = { "pan.b", "pan.c", "pan.h", "pan.m", "pan.t" };
		for (String s : spinJunk) {
			File f = new File (spinWorkDir, s);
			if (f.exists ())
				f.delete ();
		}
	}

	protected void generatePromelaCode () throws IOException {
		log ("Generating Promela code");
		Program filledProg = preprocessSketch ();
		ToSpin.printCode (filledProg, new FileOutputStream (promelaCode));
		dump ("Promela code", promelaCode, true);
	}

	protected void generateVerifierCode () throws IOException {
		log ("Generating C code for verifier");

		assert promelaCode != null && promelaCode.canRead ();

		ProcessStatus status = execDebug (spinWorkDir, 0,
				SPIN, "-a", "-v", promelaCode.getCanonicalPath ());
		assert 0 == status.exitCode;

		File panC = new File (spinWorkDir, "pan.c");
		assert panC.canRead ();
		code = panC;

		log ("Successfully generated verification code");
	}

	protected void compileVerifier () throws IOException {
		log ("Compiling the verifier");
		assert prog.canWrite () && code.canRead ();

		ProcessStatus status = execDebug (
				CC, "-w", "-o", prog.getCanonicalPath (),
				"-D_POSIX_SOURCE", "-DMEMLIM=128", "-DSAFETY", "-DNOCLAIM",
				"-DXUSAFE", "-DNOFAIR", code.getCanonicalPath ());
		assert 0 == status.exitCode;
		log ("Compilation successful");
	}

	protected void runVerifier (int timeoutMins) throws IOException {
		log ("Running the verifier");
		assert prog.canRead ();

		ProcessStatus status = execDebug (spinWorkDir, timeoutMins, prog
				.getCanonicalPath (), "-v", "-m10000", "-w19", "-n", "-c1");
		out = status.out;
		err = status.err;
		trail = "";

		trailFile = new File (promelaCode.getName () + ".trail");
		if (trailFile.exists ()) {
			status = execDebug (spinWorkDir, 0,
					prog.getCanonicalPath (), "-r",	trailFile.getCanonicalPath ());
			assert 0 == status.exitCode;
			trail = status.out;
		} else {
			// If we didn't get a Cex trace, there had better not have been
			// other errors.
			assert !hasErrors ();
		}
		if (trail.length () > 0) {
			log ("Counterexample trail:");
			log (trail);
		}
	}

	protected Program preprocessSketch () {
		// TODO: might need other passes to make SPIN verification more
		// efficient
		return (Program) sourceProg.accept (new EliminateStarStatic (holeVals));
	}

	protected ProcessStatus execDebug (String... cmdLine) {
		return execDebug (0, cmdLine);
	}

	protected ProcessStatus execDebug (int timeoutMins, String... cmdLine) {
		return execDebug (System.getProperty ("user.dir"), timeoutMins, cmdLine);
	}

	protected ProcessStatus execDebug (String workDir, int timeoutMins,
			String... cmdLine) {
		ProcessStatus status;

		try {
			String args = "";  for (String a : cmdLine)  args += a;
			log ("Launching:\t\t" + args);
			log ("In working directory:\t" + workDir);

			status = (new SynchronousTimedProcess (workDir, timeoutMins,
					cmdLine)).run ();

			log ("Process output:");
			log (status.out);
			log ("Process error:");
			log (status.err);
		} catch (Throwable t) {
			throw new RuntimeException ("Fatal error running process", t);
		}

		return status;
	}

	/* === DEBUGGING === */

	protected void log (String msg) {
		if (debug)
			System.out.println ("[SPIN][DEBUG] " + msg);
	}

	protected void dump (File f) throws IOException {
		dump (null, f, false);
	}

	protected void dump (String desc, File f, boolean linenos) throws IOException {
		if (debug) {
			System.out.println ("------------------------------------------------");
			System.out.println ("----- Dumping: "+ ((null != desc) ? desc : "??"));
			Misc.dumpStreamTo (new FileInputStream (f), System.out, linenos);
		}
	}

	/* === STATIC === */

	// XXX: assumes that 'spin' and 'gcc' are in the user's path
	public static final String	SPIN	= "spin";
	public static final String	CC		= "gcc";

	public static SpinExecuter makeExecuter (Program _prog) {
		return makeExecuter (_prog, null, false);
	}
	public static SpinExecuter makeExecuter (Program _prog, ValueOracle _oracle) {
		return makeExecuter (_prog, _oracle, false);
	}
	public static SpinExecuter makeExecuter (Program _prog,
			ValueOracle _oracle, boolean _debug) {
		return makeExecuter (_prog, _oracle, _debug, true);
	}

	public static SpinExecuter makeExecuter (Program _prog,
			ValueOracle _oracle, boolean _debug, boolean _cleanup) {
		return new SpinExecuter (_prog, _oracle, _cleanup, _debug);
	}
}