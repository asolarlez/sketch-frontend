/**
 *
 */
package streamit.frontend.spin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import streamit.frontend.nodes.Program;
import streamit.misc.Misc;
import streamit.misc.ProcessStatus;
import streamit.misc.SynchronousTimedProcess;

/**
 * Warning: this assumes that all files generated go into the same working
 * directory, the system temporary directory.  This is to hack around
 * deficiencies in Windows and SPIN.
 *
 * @author Chris Jones
 */
public class Executer {
	protected Program		sourceProg;
	protected Configuration config;
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

	protected long			cgenTimeMs;
	protected long			compileTimeMs;

	public Executer (Program _prog, Configuration _config,
					 boolean _debug, boolean _doCleanup) {
		sourceProg = _prog;
		config = _config;
		debug = _debug;
		doCleanup = _doCleanup;
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

	public long getCodegenTimeMs () {
		return cgenTimeMs;
	}

	public long getCompileTimeMs () {
		return compileTimeMs;
	}

	public boolean hasErrors () {
		return -1 == out.indexOf ("errors: 0");
	}

	protected void init () throws IOException {
		// Assumes File.createTempFile is going to use java.io.tmpdir
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
		FileOutputStream pout = new FileOutputStream (promelaCode);

		log ("Generating Promela code");
		sourceProg.accept (new PromelaCodePrinter (pout));
		pout.flush ();
		pout.close ();
		dump ("Promela code", promelaCode, true);
	}

	protected void generateVerifierCode () throws IOException {
		log ("Generating C code for verifier");

		assert promelaCode != null && promelaCode.canRead ();

		ProcessStatus status = execDebug (spinWorkDir, 0,
				SPIN, "-a", "-v", promelaCode.getName ());
		assert 0 == status.exitCode : "Error compiling Promela code";
		cgenTimeMs = status.execTimeMs;

		File panC = new File (spinWorkDir, "pan.c");
		assert panC.canRead ();
		code = panC;

		log ("Successfully generated verification code");
	}

	protected void compileVerifier () throws IOException {
		log ("Compiling the verifier");
		assert prog.canWrite () && code.canRead ();

		List<String> cmd = new ArrayList<String> ();
		cmd.add (CC); cmd.add ("-o"); cmd.add (prog.getCanonicalPath ());
		cmd.addAll (config.getCompilerFlags ());
		cmd.add (code.getCanonicalPath ());

		ProcessStatus status = execDebug (cmd);
		assert 0 == status.exitCode;
		compileTimeMs = status.execTimeMs;

		log ("Compilation successful");
	}

	protected void runVerifier (int timeoutMins) throws IOException {
		log ("Running the verifier");
		assert prog.canRead ();

		List<String> cmd = new ArrayList<String> ();
		cmd.add (prog.getCanonicalPath ());
		cmd.addAll (config.getRuntimeFlags ());
		ProcessStatus status = execDebug (spinWorkDir, timeoutMins, cmd);
		out = status.out;
		err = status.err;
		trail = "";

		trailFile = new File (promelaCode.getCanonicalPath () + ".trail");
		if (trailFile.exists ()) {
			status = execDebug (spinWorkDir, 0,
					prog.getCanonicalPath (), "-r",	trailFile.getName ());
			assert 0 == status.exitCode;
			trail = status.out;
		} else {
			// If we didn't get a cex trace, there had better not have been
			// other errors.
			assert !hasErrors ();
		}
	}

	protected ProcessStatus execDebug (String... cmdLine) {
		return execDebug (0, Arrays.asList (cmdLine));
	}

	protected ProcessStatus execDebug (List<String> cmdLine) {
		return execDebug (0, cmdLine);
	}

	protected ProcessStatus execDebug (int timeoutMins, List<String> cmdLine) {
		return execDebug (System.getProperty ("user.dir"), timeoutMins, cmdLine);
	}

	protected ProcessStatus execDebug (String workDir, int timeoutMins,
			String... cmdLine) {
		return execDebug (workDir, timeoutMins, Arrays.asList (cmdLine));
	}

	protected ProcessStatus execDebug (String workDir, int timeoutMins,
			List<String> cmdLine) {
		ProcessStatus status;

		try {
			String args = "";  for (String a : cmdLine)  args += a +" ";
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

	public static Executer makeExecuter (Program _prog) {
		return makeExecuter (_prog, new Configuration ());
	}
	public static Executer makeExecuter (Program _prog, Configuration _config) {
		return makeExecuter (_prog, _config, false, true);
	}

	public static Executer makeExecuter (Program _prog, Configuration _config,
			boolean _debug, boolean _cleanup) {
		return new Executer (_prog, _config, _debug, _cleanup);
	}
}