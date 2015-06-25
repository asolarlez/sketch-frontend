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
    }

    // main task per worker
    public SATSolutionStatistics call() {
        String prefix = "=== parallel trial w/ degree " + degree + " (" + fileIdx + ")";
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
        PrintStream out = new PrintStream(System.out, false);
        if (worker_stat != null && worker_stat.successful()) {
            synchronized (lock) {
                options.setSolFileIdx(Integer.toString(fileIdx));
                manager.found(degree);
                manager.plog(out, prefix + " start ===");
                out.println(worker_stat.out);
                manager.plog(out, prefix + " solved ===");
            }
        } else {
            String failed_solution = options.getSolutionsString(fileIdx);
            try {
                Files.delete(Paths.get(failed_solution));
            } catch (IOException e) {
                System.err.println(prefix + " can't delete " + failed_solution);
            }
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
