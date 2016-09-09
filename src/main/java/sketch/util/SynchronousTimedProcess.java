/**
 *
 */
package sketch.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import sketch.compiler.main.cmdline.SketchOptions;


/**
 * A class that launches a process synchronously, and sets a limit on the
 * process's execution time.
 *
 * @author Chris Jones
 */
public class SynchronousTimedProcess {
    protected final Process proc;
    protected File tmpFile = null;
    protected float timeoutMins;
    protected long startMs;
    public static final AtomicBoolean wasKilled = new AtomicBoolean(false);
    protected final List<String> cmdLine;

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
        this.cmdLine = cmdLine;
        for (String s : cmdLine)
            assert s != null : "Null elt of command: '" + cmdLine + "'";
        SketchOptions options = SketchOptions.getSingleton();
        if (options.debugOpts.verbosity > 2) {
            System.err.println("starting command line: " + cmdLine.toString());
        }
        ProcessBuilder pb = new ProcessBuilder (cmdLine);
        File f_workDir = new File(workDir);
        pb.directory(f_workDir);
        if (options.solverOpts.parallel) {
            String strategy = options.solverOpts.strategy.toString();
            File sktmpdir = options.sktmpdir();
            tmpFile = File.createTempFile(strategy, null, sktmpdir);
            tmpFile.deleteOnExit();
            pb.redirectOutput(ProcessBuilder.Redirect.to(tmpFile));
            pb.redirectErrorStream(true);
        }
        if (options.solverOpts.ntimes > 1) {
            pb.redirectErrorStream(true);
        }
        startMs = System.currentTimeMillis ();
        proc = pb.start ();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    proc.exitValue();
                } catch (IllegalThreadStateException e) {
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
        cmdLine = null;
    }

    @Override
    public String toString() {
        return "SynchronousTimedProcess[" + cmdLine + ", timeout=" + timeoutMins + "]";
    }

    public Process getProc() {
        return this.proc;
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
            if (tmpFile == null) {
                status.out = Misc.readStream(proc.getInputStream(), logAllOutput, null);
            }
            status.err = Misc.readStream(proc.getErrorStream(), true, System.err);
            // wait for subprocess exit first
            status.exitCode = proc.waitFor();

            status.execTimeMs = System.currentTimeMillis() - startMs;

        } catch (InterruptedException e) {
            status.exception = e;
            proc.destroy();
        } catch (IOException e) {
            status.exception = e;
        } finally {
            if (null != killer) {
                killer.abort();
                status.killedByTimeout = killer.didKill();
            }
            if (status.killedByTimeout) {
                status.execTimeMs = (long) (timeoutMins * 60 * 1000);
            }
            try {
                if (tmpFile != null) {
                    status.out = Misc.readStream(new FileInputStream(tmpFile), true, null);
                }
            } catch (IOException e) {
                status.exception = e;
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
