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
import java.io.PrintStream;

import streamit.frontend.ProcessKillerThread;
import streamit.frontend.ToSpin;
import streamit.frontend.nodes.Program;

/**
 * A class to verify a program using SPIN.
 *
 * @author Chris Jones
 */
public class SpinVerifier {
	protected Program sourceProg;
	protected File promelaCode;
	protected File code;
	protected File header;
	protected File prog;
	protected boolean doCleanup;
	protected boolean debug;

	protected int spinExitCode;
	protected String spinOut;
	protected String spinErr;

	public SpinVerifier (Program _prog, File _promelaCode,
						 File verifierProg, boolean _doCleanup, boolean _debug) {
		sourceProg = _prog;
		promelaCode = _promelaCode;
		prog = verifierProg;
		doCleanup = _doCleanup;
		debug = _debug;
	}

	public void finalize () {
		if (!doCleanup) return;

		if (null != promelaCode)	promelaCode.delete ();
		if (null != code)  			code.delete ();
		if (null != header)			header.delete ();
		if (null != prog)			prog.delete ();
	}

	/**
	 * @return true iff all assertions in this.PROG are maintained for all
	 * possible executions.
	 */
	public boolean verify () { return verify (0); }
	public boolean verify (int timeoutMins) {
		try {
			generatePromelaCode ();
			generateVerifierCode ();
			compile ();
			run (timeoutMins);
			return checkVerified ();
		} catch (Throwable t) {
			throw new RuntimeException ("Fatal file IO error", t);
		}
	}

	protected void generatePromelaCode () throws IOException {
		log ("Generating Promela code");
		ToSpin.printCode (sourceProg, new FileOutputStream (promelaCode));
		dump ("Promela code", promelaCode);
	}

	protected void generateVerifierCode () throws IOException {
		log ("Generating C code for verifier");

		assert promelaCode != null && promelaCode.canRead ();

		String[] cmdLine = { SPIN, "-a", "-v", null };
		cmdLine[3] = promelaCode.getCanonicalPath ();
		assert 0 == (debug ? exec (cmdLine) : execQuiet (cmdLine));

		File panC = new File ("./pan.c"), panH = new File ("./pan.h");
		assert panC.canRead () && panH.canRead ();
		code = panC;
		header = panH;

		log ("Successfully generated verification code");
	}

	protected void compile () throws IOException {
		log ("Compiling the verifier");
		assert prog.canWrite () && code.canRead ();

		String[] cmdLine = {
				"gcc", "-w", "-o", null,
				"-D_POSIX_SOURCE", "-DMEMLIM=128", "-DSAFETY", "-DNOCLAIM", "-DXUSAFE", "-DNOFAIR",
				null
		};
		cmdLine[3] = prog.getCanonicalPath ();
		cmdLine[cmdLine.length-1] = code.getCanonicalPath ();

		assert 0 == (debug ? exec (cmdLine) : execQuiet (cmdLine));
		log ("Compilation successful");
	}

	protected void run (int timeoutMins) throws IOException {
		log ("Running the verifier");
		assert prog.canRead ();

		String[] cmdLine = { null, "-v", "-m10000", "-w19", "-n", "-c1" };
		cmdLine[0] = prog.getCanonicalPath ();

		ByteArrayOutputStream out = new ByteArrayOutputStream ();
		ByteArrayOutputStream err = new ByteArrayOutputStream ();
		spinExitCode = exec (cmdLine, timeoutMins, debug,
							 new PrintStream (out),	new PrintStream (err));
		spinOut = out.toString ();
		spinErr = err.toString ();
		// XXX: could grab the trail file, too ...

		log ("Spin output:\n"+ spinOut);
		log ("Spin errors:\n"+ spinErr);
	}

	protected boolean checkVerified () {
		return -1 != spinOut.indexOf ("errors: 0");
	}

	public String getSpinErr () {
		return spinErr;
	}

	public String getSpinOut () {
		return spinOut;
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

	/* XXX: assumes that 'spin' is in the user's path */
	public static final String SPIN = "spin";

	public static final String CC = "gcc";

	public static SpinVerifier makeVerifier (Program _prog) {
		return makeVerifier (_prog, false);
	}
	public static SpinVerifier makeVerifier (Program _prog, boolean debug) {
		try {
			return new SpinVerifier (_prog,
					File.createTempFile ("promelaCode", ".pro"),
					File.createTempFile ("verifier", ".exe"),
					true,	// do cleanup
					debug);	// debugging on
		} catch (IOException ioe) {
			throw new RuntimeException (ioe);
		}
	}

	public int execQuiet (String[] cmd) { return exec (cmd, 0, true, System.out, System.err); }
	public int exec (String[] cmd) { return exec (cmd, 0, false, System.out, System.err); }
	public int exec (String[] cmd, int timeoutMins, boolean quiet, PrintStream out, PrintStream err) {
		ProcessKillerThread killer = null;
		try {
			if (!quiet) {
				out.print ("Executing:");
				for (int i = 0; i < cmd.length; ++i)  out.print (" "+ cmd[i]);
				out.println ("");
			}

			Process proc = Runtime.getRuntime().exec (cmd);

			if (timeoutMins > 0)
				killer = new ProcessKillerThread (proc, timeoutMins);

			if (!quiet) {
				printLines (proc.getInputStream (), out, "  ");
				printLines (proc.getErrorStream (), err, "  ");
			}

			return proc.waitFor ();
		} catch (Throwable t) {
			throw new RuntimeException (t);
		} finally {
			if (null != killer)
				killer.abort ();
		}
	}

	public void printLines (InputStream in, PrintStream out) throws IOException {
		printLines (in, out, "");
	}
	public void printLines (InputStream in, PrintStream out, String prefix) throws IOException {
		BufferedReader br = new BufferedReader (new InputStreamReader (in));
		String line = null;

		while ((line = br.readLine ()) != null)
			if(line.length() > 2)
				out.println (line);
	}
}
