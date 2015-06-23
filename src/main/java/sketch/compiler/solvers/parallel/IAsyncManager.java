package sketch.compiler.solvers.parallel;

import java.io.PrintStream;

import sketch.compiler.main.cmdline.SketchOptions;
import sketch.compiler.solvers.constructs.ValueOracle;

public interface IAsyncManager<R> {

    // interface to call back-end
    public R callSolver(ValueOracle oracle, boolean hasMinimize, float timeoutMins,
            int fileIdx);

    // interfaces to log
    public void plog(String msg);

    public void plog(PrintStream out, String msg);

    // interface to retrieve the global options
    public SketchOptions getOptions();

    // found a solution, so the manager can abort any other workers' tasks
    public void found(int degree);

    // is the job already done by another worker?
    public boolean aborted();

    // notify to the manager that the current worker just finished the task
    public void notifyWorkerDone(R stat, int degree);
}
