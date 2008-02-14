/**
 *
 */
package streamit.misc;

import java.io.File;
import java.io.IOException;


/**
 * A class that launches a process synchronously, and sets a limit on the
 * process's execution time.
 *
 * @author Chris Jones
 */
public class SynchronousTimedProcess {
	protected Process	proc;
	protected int		timeoutMins;

	public SynchronousTimedProcess (int _timeoutMins, String... cmdLine)
			throws IOException {
		this (System.getProperty ("user.dir"), _timeoutMins, cmdLine);
	}

	public SynchronousTimedProcess (String workDir, int _timeoutMins,
			String... cmdLine) throws IOException {
		ProcessBuilder pb = new ProcessBuilder (cmdLine);
		pb.directory (new File (workDir));
		proc = pb.start ();
		timeoutMins = _timeoutMins;
	}

	public SynchronousTimedProcess (Process _proc) {
		this (_proc, 0);
	}

	public SynchronousTimedProcess (Process _proc, int _timeoutMins) {
		timeoutMins = 0;
		proc = _proc;
	}

	public ProcessStatus run () throws IOException, InterruptedException {
		ProcessKillerThread killer = null;
		ProcessStatus status = new ProcessStatus ();

		try {
			if (timeoutMins > 0)
				killer = new ProcessKillerThread (proc, timeoutMins);
			status.out = Misc.readStream (proc.getInputStream ());
			status.err = Misc.readStream (proc.getErrorStream ());
			status.exitCode = proc.waitFor ();
		} finally {
			if (null != killer)
				killer.abort ();
		}

		return status;
	}
}
