/**
 *
 */
package streamit.misc;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;


/**
 * A class that launches a process synchronously, and sets a limit on the
 * process's execution time.
 *
 * @author Chris Jones
 */
public class SynchronousTimedProcess {
	protected Process	proc;
	protected int		timeoutMins;
	protected long		startMs;

	public SynchronousTimedProcess (int _timeoutMins, String... cmdLine)
			throws IOException {
		this (System.getProperty ("user.dir"), _timeoutMins, cmdLine);
	}

	public SynchronousTimedProcess (String workDir, int _timeoutMins,
			String... cmdLine) throws IOException {
		this (workDir, _timeoutMins, Arrays.asList (cmdLine));
	}

	public SynchronousTimedProcess (String workDir, int _timeoutMins,
				List<String> cmdLine) throws IOException {
		ProcessBuilder pb = new ProcessBuilder (cmdLine);
		pb.directory (new File (workDir));
		startMs = System.currentTimeMillis ();
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
			status.execTimeMs = System.currentTimeMillis () - startMs;
		} finally {
			if (null != killer)
				killer.abort ();
		}

		return status;
	}
}
