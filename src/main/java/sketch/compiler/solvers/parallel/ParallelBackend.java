package sketch.compiler.solvers.parallel;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

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
    protected boolean parallel_failed = false;
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
        if (options.solverOpts.pCPUs <= 0) {
            // to not exhaust the system, use three quarters of available cores
            int three_q = (int) (Runtime.getRuntime().availableProcessors() * 0.75);
            cpu = Math.max(1, three_q);
        } else {
            cpu = Math.max(1, options.solverOpts.pCPUs);
        }
        // at least 8 samples per round
        test_trial_max = Math.max(8, (cpu / 2));

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
            String prefix =
                    "=== parallel trial w/ degree " + options.solverOpts.randdegree + " ("
                            + fileIdx + ")";

            private void cleanUpTmpSol() {
                String tmp_solution = options.getSolutionsString(fileIdx);
                try {
                    Files.delete(Paths.get(tmp_solution));
                } catch (IOException e) {
                    System.err.println(prefix + " can't delete " + tmp_solution);
                }
            }

            // main task per worker
            public SATSolutionStatistics call() {
                synchronized (lock) {
                    if (parallel_solved || parallel_failed) {
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
                // double-check the result is not null
                if (worker_stat == null)
                    return worker_stat;
                // double-check whether a solution/UNSAT is already determined

                final int ALREADY_DONE = 0;
                final int THIS_SOLVED = 1;
                final int THIS_UNSAT = 2;
                int state = -1;
                synchronized (lock) {
                    if (parallel_solved || parallel_failed) {
                        state = ALREADY_DONE;
                    } else {
                        if (worker_stat.successful()) {
                            state = THIS_SOLVED;
                            parallel_solved = true;
                        }
                        if (worker_stat.unsat()) {
                            state = THIS_UNSAT;
                            parallel_failed = true;
                        }
                    }
                }
                // Only one thread can be in a state that is either THIS_SOLVED
                // or THIS_UNSAT.
                switch (state) {
                case ALREADY_DONE:
                    synchronized (lock) {
                        plog(prefix + " done, but aborted ===");
                    }
                    cleanUpTmpSol();
                    return null;
                case THIS_SOLVED: {
                    PrintStream out = new PrintStream(System.out, false);
                    synchronized (lock) {
                        options.setSolFileIdx(Integer.toString(fileIdx));
                        plog(out, prefix + " start ===");
                        out.println(worker_stat.out);
                        log(2, "Stats for last run:\n" + worker_stat);
                        plog(out, prefix + " solved ===");
                        out.flush();
                    }
                    return worker_stat;
                }
                case THIS_UNSAT: {
                    PrintStream out = new PrintStream(System.out, false);
                    cleanUpTmpSol();
                    synchronized (lock) {
                        plog(out, prefix + " start ===");
                        out.println(worker_stat.out);
                        log(2, "Stats for last run:\n" + worker_stat);
                        plog(out, prefix + " failed ===");
                        out.flush();
                    }
                    return worker_stat;
                }
                default:
                    PrintStream out = new PrintStream(System.out, false);
                    cleanUpTmpSol();
                    synchronized (lock) {
                        plog(out, prefix + " start ===");
                        out.println(worker_stat.out);
                        log(2, "Stats for last run:\n" + worker_stat);
                        plog(out, prefix + " didn't succeed or fail ===");
                        out.flush();
                    }
                    return worker_stat;
                }
            }
        };
    }

    float adaptiveTimeoutMins;

    ExecutorService es;
    CompletionService<SATSolutionStatistics> ces;
    List<Future<SATSolutionStatistics>> futures;

    // will be reused by strategy-based parallel running
    protected List<SATSolutionStatistics> sync_parallel_solve(ValueOracle oracle,
            boolean hasMinimize, float timeoutMins, int max_trials)
    {
        List<SATSolutionStatistics> results = new ArrayList<SATSolutionStatistics>();

        // generate worker pool and managed executor
        es = Executors.newFixedThreadPool(cpu);
        ces = new ExecutorCompletionService<SATSolutionStatistics>(es);
        // place to maintain future parallel tasks
        futures = new ArrayList<Future<SATSolutionStatistics>>(max_trials);
        try {
            // submit parallel tasks
            int nTrial = 0;
            for (nTrial = 0; nTrial < max_trials; nTrial++) {
                // while submitting tasks, check whether it's already solved
                synchronized (lock) {
                    if (parallel_solved || parallel_failed) {
                        es.shutdown(); // no more tasks accepted
                        break;
                    }
                }
                Callable<SATSolutionStatistics> c =
                        createWorker(oracle, minimize, timeoutMins, nTrial);
                try {
                    Future<SATSolutionStatistics> f = ces.submit(c);
                    futures.add(f);
                } catch (RejectedExecutionException e) {
                    plog("failed to submit the task (" + nTrial + ")");
                    break;
                }
            }
            // plog("=== submitted parallel trials: " + nTrials + " ===");
            adaptiveTimeoutMins = timeoutMins;
            // check tasks' results in the order of their completion
            for (int i = 0; i < nTrial; i++) {
                // could be shut down by the timeout monitor
                if (es.isShutdown() || es.isTerminated())
                    break;
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
                    // found a worker that found a solution
                    if (r.successful()) {
                        plog("=== resolved within " + (i + 1) + " complete parallel trial(s)");
                        es.shutdownNow(); // attempts to stop active tasks
                        // break the iteration and go to finally block
                        break;
                    }
                    // found a worker that found the problem UNSAT
                    else if (r.unsat()) {
                        plog("=== resolved within " + (i + 1) + " complete parallel trial(s)");
                        es.shutdownNow(); // attempts to stop active tasks
                        // break the iteration and go to finally block
                        break;
                    }
                } catch (CancellationException e) {
                    plog(e.toString());
                } catch (ExecutionException e) {
                    plog(e.toString());
                } catch (InterruptedException e) {
                    plog(e.toString());
                } catch (TimeoutException e) {
                    plog(e.toString());
                }
            }
        } finally {
            cleanUpPool();
        }
        return results;
    }

    void cleanUpPool() {
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

    class TimeoutMonitor extends Thread {
        ParallelBackend mainThread;
        float timeoutMins;
        boolean aborted;

        TimeoutMonitor(ParallelBackend mainThread, float timeoutMins) {
            this.mainThread = mainThread;
            this.timeoutMins = timeoutMins;
            this.aborted = false;
        }

        boolean alive() {
            synchronized (lock) {
                return !aborted;
            }
        }

        void abort() {
            synchronized (lock) {
                aborted = true;
            }
            interrupt();
        }

        public void run() {
            try {
                sleep((long) (timeoutMins * 60 * 1000));
            } catch (InterruptedException e) {}
            if (aborted)
                return;
            plog("Time limit exceeded!");
            mainThread.cleanUpPool();
        }
    }

    @Override
    protected boolean solve(ValueOracle oracle, boolean hasMinimize, float timeoutMins) {
        int pTrials = options.solverOpts.pTrials;
        TimeoutMonitor monitor = null;
        // timeout precedes # of trials
        // i.e., if timeout is set, run virtually infinitely
        if (timeoutMins > 0) {
            // shouldn't be that big due to OutOfMemoryError, heap space, etc.
            pTrials = cpu * 32 * 32;
            monitor = new TimeoutMonitor(this, timeoutMins);
            monitor.start();
        } else if (pTrials <= 0) {
            pTrials = cpu; // * 32 * 3;
        }

        sync_parallel_solve(oracle, hasMinimize, timeoutMins, pTrials);
        if (monitor != null && monitor.alive()) {
            monitor.abort();
        }
        return parallel_solved;
    }

    @Override
    protected boolean checkBeforeRunning(SynchronousTimedProcess proc) {
        // maintain CEGIS process, to terminate it directly
        // when another worker already found a solution. If a solution has
        // already been found, this method returns false.

        if (es.isShutdown()) {
            return false;
        }

        synchronized (lock) {
            cegiss.add(proc.getProc());
        }
        return true;
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
