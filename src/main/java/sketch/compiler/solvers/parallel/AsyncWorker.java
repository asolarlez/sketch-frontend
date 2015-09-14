package sketch.compiler.solvers.parallel;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

import sketch.compiler.main.cmdline.SketchOptions;
import sketch.compiler.solvers.SATSolutionStatistics;
import sketch.compiler.solvers.constructs.ValueOracle;
import sketch.util.exceptions.SketchSolverException;

public class AsyncWorker implements Callable<SATSolutionStatistics> {

    private static Object lock = new Object();

    IAsyncManager<SATSolutionStatistics> manager;
    SketchOptions options;

    ValueOracle oracle;
    boolean hasMinimize;
    float timeoutMins;
    int fileIdx;
    int degree;

    String prefix;

    public AsyncWorker(IAsyncManager<SATSolutionStatistics> manager, ValueOracle oracle,
            boolean hasMinimize, float timeoutMins, int fileIdx, int degree)
    {
        this.manager = manager;
        this.options = manager.getOptions();

        this.oracle = oracle;
        this.hasMinimize = hasMinimize;
        this.timeoutMins = timeoutMins;
        this.fileIdx = fileIdx;
        this.degree = degree;

        this.prefix = "=== parallel trial w/ degree " + degree + " (" + fileIdx + ")";
    }

    protected void cleanUpTmpSol() {
        String tmp_solution = options.getSolutionsString(fileIdx);
        try {
            Files.delete(Paths.get(tmp_solution));
        } catch (IOException e) {
            System.err.println(prefix + " can't delete " + tmp_solution);
        }
    }

    // main task per worker
    public SATSolutionStatistics call() {
        if (manager.aborted()) {
            manager.plog(prefix + " aborted ===");
            return null;
        }
        SATSolutionStatistics worker_stat = null;
        try {
            int old_d = options.solverOpts.randdegree;
            options.solverOpts.randdegree = this.degree;

            int old_v = options.debugOpts.verbosity;
            if (old_v < 5) {
                options.debugOpts.verbosity = 5; // to see hole concretization info
            }

            worker_stat = manager.callSolver(oracle, hasMinimize, timeoutMins, fileIdx);

            options.solverOpts.randdegree = old_d;
            options.debugOpts.verbosity = old_v;
        } catch (SketchSolverException e) {
            e.setBackendTempPath(options.getTmpSketchFilename());
        }
        // double-check the result is not null
        if (worker_stat == null)
            return worker_stat;
        // double-check whether a solution/UNSAT is already determined
        if (manager.aborted()) {
            manager.plog(prefix + " done, but aborted ===");
            cleanUpTmpSol();
            return null;
        }

        PrintStream out = new PrintStream(System.out, false);
        if (worker_stat.successful()) {
            synchronized (lock) {
                options.setSolFileIdx(Integer.toString(fileIdx));
                manager.found(degree);
                manager.plog(out, prefix + " start ===");
                out.println(worker_stat.out);
                manager.plog(out, prefix + " solved ===");
            }
        } else {
            if (worker_stat.unsat()) {
                manager.end(degree);
            }
            cleanUpTmpSol();
            synchronized (lock) {
                manager.plog(out, prefix + " start ===");
                out.println(worker_stat.out);
                manager.plog(out, prefix + " failed ===");
            }
        }
        out.flush();
        manager.notifyWorkerDone(worker_stat, this.degree);
        return worker_stat;
    }
}
