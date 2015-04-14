package sketch.compiler.solvers.parallel;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.dataflow.recursionCtrl.RecursionControl;
import sketch.compiler.main.cmdline.SketchOptions;
import sketch.compiler.solvers.SATBackend;
import sketch.compiler.solvers.SATSolutionStatistics;
import sketch.compiler.solvers.constructs.ValueOracle;
import sketch.util.Misc;
import sketch.util.SynchronousTimedProcess;
import sketch.util.exceptions.SketchSolverException;

public class ParallelBackend extends SATBackend {

    protected boolean parallel_solved = false;
    private List<Process> cegiss;
    protected Object lock;

    protected int cpu;
    protected int test_trial_max;

    public ParallelBackend(SketchOptions options, RecursionControl rcontrol,
            TempVarGen varGen)
    {
        super(options, rcontrol, varGen);
        lock = new Object();
        cegiss = new ArrayList<Process>();
        if (options.solverOpts.pCPUs < 0) {
            int three_q = (int) (Runtime.getRuntime().availableProcessors() * 0.75);
            cpu = Math.max(1, three_q);
        } else {
            cpu = Math.max(1, options.solverOpts.pCPUs);
        }
        test_trial_max = Math.max(8, cpu);

        // if seed is given (to reproduce certain experiments, use it as-is
        // otherwise, use a random seed
        if (options.solverOpts.seed == 0) {
            options.solverOpts.seed = (int) (System.currentTimeMillis());
        }
    }

    protected Callable<SATSolutionStatistics> createWorker(final ValueOracle oracle,
            boolean hasMinimize, final float timeoutMins, final int fileIdx)
    {
        return new Callable<SATSolutionStatistics>() {
            // main task per worker
            public SATSolutionStatistics call() {
                String prefix =
                        "=== parallel trial w/ degree " + options.solverOpts.randdegree +
                                " (" + fileIdx + ")";
                synchronized (lock) {
                    if (parallel_solved) {
                        plog(prefix + " aborted ===");
                        return null;
                    }
                }
                SATSolutionStatistics worker_stat = null;
                try {
                    worker_stat =
                            incrementalSolve(oracle, minimize, timeoutMins, fileIdx);
                } catch (SketchSolverException e) {
                    e.setBackendTempPath(options.getTmpSketchFilename());
                }
                PrintStream out = new PrintStream(System.out, false);
                if (worker_stat != null && worker_stat.successful()) {
                    synchronized (lock) {
                        parallel_solved = true;
                    }
                    synchronized (lock) {
                        plog(out, prefix + " start ===");
                        out.println(worker_stat.out);
                        plog(out, prefix + " solved ===");
                    }
                } else {
                    String failed_solution = options.getSolutionsString(fileIdx);
                    try {
                        Files.delete(Paths.get(failed_solution));
                    } catch (IOException e) {
                        System.err.println(prefix + " can't delete " + failed_solution);
                    }
                    synchronized (lock) {
                        plog(out, prefix + " start ===");
                        out.println(worker_stat.out);
                        plog(out, prefix + " failed ===");
                    }
                }
                out.flush();
                return worker_stat;
            }
        };
    }

    float adaptiveTimeoutMins;

    // will be reused by strategy-based parallel running
    protected List<SATSolutionStatistics> sync_parallel_solve(ValueOracle oracle,
            boolean hasMinimize, float timeoutMins, int max_trials)
    {
        List<SATSolutionStatistics> results = new ArrayList<SATSolutionStatistics>();

        // generate worker pool and managed executor
        ExecutorService es = Executors.newFixedThreadPool(cpu);
        CompletionService<SATSolutionStatistics> ces =
                new ExecutorCompletionService<SATSolutionStatistics>(es);
        // place to maintain future parallel tasks
        List<Future<SATSolutionStatistics>> futures =
                new ArrayList<Future<SATSolutionStatistics>>(max_trials);
        try {
            // submit parallel tasks
            int nTrials = 0;
            for (nTrials = 0; nTrials < max_trials; nTrials++) {
                // while submitting tasks, check whether it's already solved
                synchronized (lock) {
                    if (parallel_solved) {
                        es.shutdown(); // no more tasks accepted
                        break;
                    }
                }
                Callable<SATSolutionStatistics> c =
                        createWorker(oracle, minimize, timeoutMins, nTrials);
                Future<SATSolutionStatistics> f = ces.submit(c);
                futures.add(f);
            }
            // plog("=== submitted parallel trials: " + nTrials + " ===");
            adaptiveTimeoutMins = timeoutMins;
            // check tasks' results in the order of their completion
            for (int i = 0; i < nTrials; i++) {
                try {
                    SATSolutionStatistics r =
                            ces.take().get((long) adaptiveTimeoutMins, TimeUnit.MINUTES);
                    if (r == null) continue; // means, aborted
                    results.add(r);
                    // if timed out during the learning phase, extend it
                    if (r.killedByTimeout && stage == STAGE.LEARNING) {
                        adaptiveTimeoutMins = options.solverOpts.extendPTimeout();
                        plog("=== timeout extended to " + adaptiveTimeoutMins);
                    }
                    // found a worker that finishes the job
                    if (r != null && r.successful()) {
                        plog("=== resolved within " + (i + 1) + " complete parallel trial(s)");
                        es.shutdownNow(); // attempts to stop active tasks
                        // break the iteration and go to finally block
                        break;
                    }
                } catch (InterruptedException ignore) {
                } catch (ExecutionException ignore) {
                } catch (TimeoutException ignore) {
                }
            }
        } finally {
            // double-check the thread pool has been shut down
            // if *all* trials failed, it wasn't shut down
            if (!es.isShutdown())
                es.shutdownNow();
            // cancel any remaining tasks
            for (Future<SATSolutionStatistics> f : futures) {
                f.cancel(true);
            }
            // terminate any alive CEGIS processes
            terminateSubprocesses();
        }
        return results;
    }

    @Override
    protected boolean solve(ValueOracle oracle, boolean hasMinimize, float timeoutMins) {
        int pTrials = options.solverOpts.pTrials;
        if (pTrials < 0) {
            pTrials = cpu * 32 * 3;
        }

        sync_parallel_solve(oracle, hasMinimize, timeoutMins, pTrials);
        return parallel_solved;
    }

    @Override
    protected void beforeRunSolver(SynchronousTimedProcess proc) {
        // maintain CEGIS process, to terminate it directly
        // when another worker already found a solution
        synchronized (lock) {
            cegiss.add(proc.getProc());
        }
    }

    public void terminateSubprocesses() {
        synchronized (lock) {
            for (Process p : cegiss) {
                try {
                    p.exitValue();
                } catch (IllegalThreadStateException e) {
                    plog("destroying " + p);
                    p.destroy(); // if still running, kill the process
                }
            }
            cegiss.clear();
        }
    }

    public enum STAGE {
        LEARNING, TESTING
    };

    public static STAGE stage;

    @Override
    protected SATSolutionStatistics parseStats(String out) {
        SATSolutionStatistics stat = super.parseStats(out);
        // parsing holes' range and calculate search space might be expensive
        // so, do the calculation only if it is in the learning phase
        if (stage == STAGE.LEARNING) {
            stat.probability = 1.0;
            List<String> res;
            res = Misc.search(out, "(H__\\S+): replacing with value \\d+ bnd= (\\d+)");
            if (res != null) {
                for (int i = 0; i < res.size(); i += 2) {
                    // String hole = res.get(i);
                    int bound = Integer.parseInt(res.get(i + 1));
                    if (bound <= 1)
                        continue;
                    stat.probability /= bound;
                }
            }
        }
        return stat;
    }

    protected List<SATSolutionStatistics> runSyncTrials(ValueOracle oracle,
            boolean hasMinimize, int d)
    {
        return runSyncTrials(oracle, hasMinimize, d, test_trial_max);
    }

    protected List<SATSolutionStatistics> runSyncTrials(ValueOracle orcle,
            boolean hasMinimize, int d, int n)
    {
        int old_d = options.solverOpts.randdegree;
        options.solverOpts.randdegree = d;

        long old_m = options.solverOpts.memLimit;
        options.solverOpts.memLimit = 4 * 1024 * 1024 * 1024; // 4G

        int old_v = options.debugOpts.verbosity;
        if (old_v < 5) {
            options.debugOpts.verbosity = 5; // to see hole concretization info
        }

        List<SATSolutionStatistics> stats =
                sync_parallel_solve(oracle, hasMinimize, options.solverOpts.pTimeout, n);

        options.solverOpts.randdegree = old_d;
        options.solverOpts.memLimit = old_m;
        options.debugOpts.verbosity = old_v;
        return stats;
    }

}
