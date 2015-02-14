/**
 *
 */
package sketch.util;

import sketch.compiler.main.cmdline.SketchOptions;

/**
 * A thread that tracks a running process.  If the process exceeds a time
 * limit, it is killed off.
 *
 * @author Chris Jones
 */
public class ProcessKillerThread extends Thread {
    private float fTimeout;
    private final Process proc;
    private volatile boolean aborted = false;
    private boolean killed = false;

    SketchOptions options;

    public ProcessKillerThread(Process p, float timeoutMinutes) {
        proc = p;
        fTimeout = timeoutMinutes;
        setDaemon(true);
        options = SketchOptions.getSingleton();
    }

    public void abort() {
        aborted = true;
        interrupt();
    }

    public boolean didKill () {
        return killed;
    }

    public void run() {
        try {
            sleep((long)(fTimeout * 60 * 1000));
            if (options.solverOpts.parallel) {
                float additional = 0;
                while ((additional = options.solverOpts.checkPTimeout(fTimeout)) > 0) {
                    fTimeout += additional;
                    sleep((long)(additional * 60 * 1000));
                }
            }
        }
        catch (InterruptedException e) {
        }
        if (aborted) return;
        System.out.println("Time limit exceeded!");
        killed = true;
        proc.destroy();
    }
}
