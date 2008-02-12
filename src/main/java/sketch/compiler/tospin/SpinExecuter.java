/**
 *
 */
package streamit.frontend.tospin;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;

import streamit.frontend.ProcessKillerThread;
import streamit.frontend.ToSpin;
import streamit.frontend.nodes.Program;
import streamit.frontend.stencilSK.EliminateStarStatic;
import streamit.frontend.tosbit.ValueOracle;

/**
 * @author Chris Jones
 */
public class SpinExecuter {
	protected Program sourceProg;
	protected ValueOracle holeVals;

	protected File promelaCode;
	protected File code;
	protected File prog;
	protected File trailFile;
	protected boolean doCleanup;
	protected boolean debug;

	protected int exitCode;
	protected String out;
	protected String err;
	protected String trail;

	public SpinExecuter (Program _prog,
						 ValueOracle _holeVals,
						 File _promelaCode,
						 File verifierProg,
						 boolean _doCleanup, boolean _debug) {
		sourceProg = _prog;
		holeVals = _holeVals;
		promelaCode = _promelaCode;
		prog = verifierProg;
		doCleanup = _doCleanup;
		debug = _debug;
	}

	public void run () throws IOException { run (0); }
	public void run (int timeoutMins) throws IOException {
		try {
			generatePromelaCode ();
			generateVerifierCode ();
			compileVerifier ();
			runVerifier (timeoutMins);
		} finally {
			if (!doCleanup) return;

			File[] toDelete = { promelaCode, prog, trailFile };
			for (File f : toDelete)
				if (null != f && f.exists ())  f.delete ();

			String[] spinJunk = { "pan.b", "pan.c", "pan.h", "pan.m", "pan.t" };
			for (String s : spinJunk) {
				File f = new File (s);
				if (f.exists ())  f.delete ();
			}
		}
	}

	public String getOutput () { return out; }
	public String getErrorOutput () { return err; }
	public String getTrail () { return trail; }

	public boolean hasErrors () {
		return -1 == out.indexOf ("errors: 0");
	}

	protected void generatePromelaCode () throws IOException {
		log ("Generating Promela code");
		Program filledProg = preprocessSketch ();
		ToSpin.printCode (filledProg, new FileOutputStream (promelaCode));
		dump ("Promela code", promelaCode);
	}

	protected void generateVerifierCode () throws IOException {
		log ("Generating C code for verifier");

		assert promelaCode != null && promelaCode.canRead ();

		String[] cmdLine = { SPIN, "-a", "-v", null };
		cmdLine[3] = promelaCode.getCanonicalPath ();
		assert 0 == execDebug (cmdLine);

		File panC = new File ("pan.c");
		assert panC.canRead ();
		code = panC;

		log ("Successfully generated verification code");
	}

	protected void compileVerifier () throws IOException {
		log ("Compiling the verifier");
		assert prog.canWrite () && code.canRead ();

		String[] cmdLine = {
				CC, "-w", "-o", null,
				"-D_POSIX_SOURCE", "-DMEMLIM=128", "-DSAFETY", "-DNOCLAIM", "-DXUSAFE", "-DNOFAIR",
				null
		};
		cmdLine[3] = prog.getCanonicalPath ();
		cmdLine[cmdLine.length-1] = code.getCanonicalPath ();

		assert 0 == execDebug (cmdLine);
		log ("Compilation successful");
	}

	protected void runVerifier (int timeoutMins) throws IOException {
		log ("Running the verifier");
		assert prog.canRead ();

		String[] cmdLine = { null, "-v", "-m10000", "-w19", "-n", "-c1" };
		cmdLine[0] = prog.getCanonicalPath ();

		ByteArrayOutputStream outStream = new ByteArrayOutputStream ();
		ByteArrayOutputStream errStream = new ByteArrayOutputStream ();
		exitCode = execDebug (cmdLine, timeoutMins,
						 	  new PrintStream (outStream),
						 	  new PrintStream (errStream));
		out = outStream.toString ();
		err = errStream.toString ();
		trail = "";

		trailFile = new File (promelaCode.getName () + ".trail");
		if (trailFile.exists ()) {
			String[] trailCmd = { null, "-r", null };
			ByteArrayOutputStream trailStream = new ByteArrayOutputStream ();

			trailCmd[0] = prog.getCanonicalPath ();
			trailCmd[2] = trailFile.getCanonicalPath ();
			assert 0 == execDebug (cmdLine, new PrintStream (trailStream));
			trail = trailStream.toString ();
		} else {
			// If we didn't get a Cex trace, there had better not have been
			// other errors.
			assert !hasErrors ();
		}

		log ("Spin output:\n"+ out);
		log ("Spin errors:\n"+ err);
		if (trail.length () > 0) { log ("Spin trail:\n"+ trail); }
	}

	protected Program preprocessSketch () {
		// TODO: might need other passes to make SPIN verification more efficient
		return (Program) sourceProg.accept (new EliminateStarStatic (holeVals));
	}

	protected int execDebug (String[] cmdLine) {
		return execDebug (cmdLine, 0, QOStream.QUIET_PSTREAM, QOStream.QUIET_PSTREAM);
	}
	protected int execDebug (String[] cmdLine, PrintStream out) {
		return execDebug (cmdLine, 0, out, QOStream.QUIET_PSTREAM);
	}
	protected int execDebug (String[] cmdLine, int timeoutMins, PrintStream out, PrintStream err) {
		return exec (cmdLine, timeoutMins, !debug, out, err);
	}
	/* === DEBUGGING === */

	protected void log (String msg) {
		if (debug) {
			System.out.println ("------------------------------------------------");
			System.out.println (msg);
		}
	}

	protected void dump (File f) throws IOException { dump (null, f); }
	protected void dump (String desc, File f) throws IOException {
		if (debug) {
			System.out.println ("------------------------------------------------");
			if (null != desc) System.out.println ("----- Dumping: "+ desc);
			printLines (new FileInputStream (f), System.out);
		}
	}

	/* === STATIC === */

	// XXX: assumes that 'spin' and 'gcc' are in the user's path
	public static final String SPIN = "spin";
	public static final String CC = "gcc";

	public static SpinExecuter makeExecuter (Program _prog) {
		return makeExecuter (_prog, null, false);
	}
	public static SpinExecuter makeExecuter (Program _prog, ValueOracle _oracle) {
		return makeExecuter (_prog, _oracle, false);
	}
	public static SpinExecuter makeExecuter (Program _prog, ValueOracle _oracle,
											 boolean _debug) {
		return makeExecuter (_prog, _oracle, _debug, true);
	}
	public static SpinExecuter makeExecuter (Program _prog, ValueOracle _oracle,
											 boolean _debug, boolean _cleanup) {
		try {
			return new SpinExecuter (_prog, _oracle,
					File.createTempFile ("promelaCode", ".pml"),
					File.createTempFile ("verifier", ".exe"),
					_cleanup, _debug);
		} catch (IOException ioe) {
			throw new RuntimeException (ioe);
		}

	}

	/* === HELPER CODE (SHOULD BE FACTORED OUT) === */

	public static class QOStream extends OutputStream {
		public static final OutputStream QUIET_OSTREAM = new QOStream ();
		public static final PrintStream QUIET_PSTREAM = new PrintStream (QUIET_OSTREAM);

		public void flush () { }
		public void close () { }
		public void write (int arg) { }
		public void write (String arg) { }
	}

	public int execQuiet (String[] cmd)	{
		return exec (cmd, 0, true, System.out, System.err);
	}
	public int exec (String[] cmd) {
		return exec (cmd, 0, false, System.out, System.err);
	}
	public int exec (String[] cmd, PrintStream out) {
		return exec (cmd, 0, false, out, System.err);
	}
	public int exec (String[] cmd, PrintStream out, PrintStream err) {
		return exec (cmd, 0, false, out, err);
	}
	public int exec (String[] cmd, int timeoutMins, boolean quiet, PrintStream out, PrintStream err) {
		ProcessKillerThread killer = null;
		try {
			if (!quiet) {
				System.out.print ("Executing:");
				for (int i = 0; i < cmd.length; ++i)  System.out.print (" "+ cmd[i]);
				System.out.println ("");
			}

			Process proc = Runtime.getRuntime().exec (cmd);

			if (timeoutMins > 0)
				killer = new ProcessKillerThread (proc, timeoutMins);

			if (!quiet) System.out.println ("Output:");
			printLines (proc.getInputStream (), out, quiet, "  ");
			if (!quiet) System.out.println ("Errors:");
			printLines (proc.getErrorStream (), err, quiet, "  ");

			return proc.waitFor ();
		} catch (Throwable t) {
			throw new RuntimeException (t);
		} finally {
			if (null != killer)
				killer.abort ();
		}
	}

	public void printLines (InputStream in, PrintStream out) throws IOException {
		printLines (in, out, true, "");
	}
	public void printLines (InputStream in, PrintStream out, boolean quiet) throws IOException {
		printLines (in, out, quiet, "");
	}
	public void printLines (InputStream in, PrintStream out, boolean quiet, String prefix) throws IOException {
		BufferedReader br = new BufferedReader (new InputStreamReader (in));
		String line = null;

		while ((line = br.readLine ()) != null) {
			if(line.length() > 2) {
				out.println (prefix + line);
				if (!quiet)
					System.out.println (prefix + line);
			}
		}
	}
}
