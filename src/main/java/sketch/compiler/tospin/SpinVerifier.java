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
	protected SpinExecuter spin;

	public SpinVerifier (Program prog) {
		spin = SpinExecuter.makeExecuter (prog);
	}

	/**
	 * @return true iff all assertions in this.PROG are maintained for all
	 * possible executions.
	 */
	public boolean verify () { return verify (0); }
	public boolean verify (int timeoutMins) {
		try { spin.run (timeoutMins); } catch (IOException ioe) {
			throw new RuntimeException (ioe);
		}
		return !hasErrors ();
	}

	protected boolean hasErrors () {
		return -1 == spin.getOutput ().indexOf ("errors: 0");
	}

	public String getOutput () {
		return spin.getOutput ();
	}

	public static SpinVerifier makeVerifier (Program prog) {
		return new SpinVerifier (prog);
	}
}