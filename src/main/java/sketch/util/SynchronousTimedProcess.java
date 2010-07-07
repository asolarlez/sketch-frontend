/**
 *
 */
package sketch.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import sketch.compiler.main.seq.SequentialSketchOptions;


/**
 * A class that launches a process synchronously, and sets a limit on the
 * process's execution time.
 *
 * @author Chris Jones
 */
public class SynchronousTimedProcess {
    protected final Process proc;
	protected float		timeoutMins;
	protected long		startMs;
    public static final AtomicBoolean wasKilled = new AtomicBoolean(false);

	public SynchronousTimedProcess (float timeoutMins, String... cmdLine)
			throws IOException {
		this (System.getProperty ("user.dir"), timeoutMins, cmdLine);
	}

	public SynchronousTimedProcess (String workDir, float timeoutMins,
			String... cmdLine) throws IOException {
		this (workDir, timeoutMins, Arrays.asList (cmdLine));
	}

	public SynchronousTimedProcess (String workDir, float timeoutMins,
				List<String> cmdLine) throws IOException {
        for (String s : cmdLine)
            assert s != null : "Null elt of command: '" + cmdLine + "'";
        if (SequentialSketchOptions.getSingleton().debugOpts.verbosity > 2) {
            System.err.println("starting command line: " + cmdLine.toString());
        }
		ProcessBuilder pb = new ProcessBuilder (cmdLine);
		pb.directory (new File (workDir));
		startMs = System.currentTimeMillis ();
		proc = pb.start ();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    proc.exitValue();
                } catch (IllegalThreadStateException e) {
                    DebugOut.printDebug("killing thread");
                    wasKilled.set(true);
                    proc.destroy();
                }
            }
        });
		this.timeoutMins = timeoutMins;
	}

	public SynchronousTimedProcess (Process _proc) {
		this (_proc, 0);
	}

	public SynchronousTimedProcess (Process _proc, int _timeoutMins) {
		timeoutMins = 0;
		proc = _proc;
	}

    public ProcessStatus run(boolean logAllOutput) {
        ProcessKillerThread killer = null;
        ProcessStatus status = new ProcessStatus();
        System.gc();

        try {
            if (timeoutMins > 0) {
                killer = new ProcessKillerThread(proc, timeoutMins);
                killer.start();
            }

            status.out = Misc.readStream(proc.getInputStream(), logAllOutput);
            status.err = Misc.readStream(proc.getErrorStream(), true);
            status.exitCode = proc.waitFor();
            status.execTimeMs = System.currentTimeMillis() - startMs;

        } catch (Throwable e) {
            status.exception = e;
        } finally {
            if (null != killer) {
                killer.abort();
                status.killedByTimeout = killer.didKill();
            }
        }

        return status;
    }

	public OutputStream getOutputStream() {
		
		return proc.getOutputStream();
	}
	
	public InputStream getErrorStream() {
		return proc.getErrorStream();
	}
	
	public InputStream getInputStream() {
		return proc.getInputStream();
	}
	
}
