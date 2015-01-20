package sketch.compiler.solvers.parallel;

import java.io.IOException;
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
import sketch.compiler.solvers.constructs.ValueOracle;
import sketch.util.SynchronousTimedProcess;
import sketch.util.exceptions.SketchSolverException;

public class ParallelBackend extends SATBackend {

    public ParallelBackend(SketchOptions options, RecursionControl rcontrol,
            TempVarGen varGen)
    {
        super(options, rcontrol, varGen);
    }

    private boolean parallel_solved = false;
    private List<Process> cegiss;
    private Object lock = new Object();

    private Callable<Boolean> createWorker(final ValueOracle oracle, boolean hasMinimize,
            float timeoutMins, final int fileIdx)
    {
        return new Callable<Boolean>() {
            // main task per worker
            public Boolean call() {
                synchronized (lock) {
                    if (parallel_solved) {
                        return false;
                    }
                }
                String prefix = "=== parallel trial (" + fileIdx + ")";
                plog(prefix + " start ===");
                boolean worker_ret = false;
                try {
                    worker_ret = incrementalSolve(oracle, minimize, options.solverOpts.timeout, fileIdx);
                } catch (SketchSolverException e) {
                    e.setBackendTempPath(options.getTmpSketchFilename());
                }
                if (worker_ret) {
                    plog(prefix + " solved ===");
                    synchronized (lock) {
                        parallel_solved = true;
                    }
                } else {
                    plog(prefix + " failed ===");
                    String failed_solution = options.getSolutionsString(fileIdx);
                    try {
                        Files.delete(Paths.get(failed_solution));
                    } catch (IOException e) {
                        System.err.println(prefix + " can't delete " + failed_solution);
                    }
                }
                return worker_ret;
            }
        };
    }

    @Override
    protected boolean solve(ValueOracle oracle, boolean hasMinimize, float timeoutMins) {
        boolean worked = false;
        // if seed is given (to reproduce certain experiments, use it as-is
        // otherwise, use a random seed
        if (options.solverOpts.seed == 0) {
            options.solverOpts.seed = (int) (System.currentTimeMillis());
        }

        int three_q = (int) (Runtime.getRuntime().availableProcessors() * 0.83);
        int cpu = Math.max(1, three_q);
        int pTrials = options.solverOpts.pTrials;
        if (pTrials < 0) {
            pTrials = cpu * 32 * 3;
        }

        synchronized (lock) {
            cegiss = new ArrayList<Process>();
        }

        // generate worker pool and managed executor
        ExecutorService es = Executors.newFixedThreadPool(cpu);
        CompletionService<Boolean> ces = new ExecutorCompletionService<Boolean>(es);
        // place to maintain future parallel tasks
        List<Future<Boolean>> futures = new ArrayList<Future<Boolean>>(pTrials);
        try {
            // submit parallel tasks
            int nTrials = 0;
            for (nTrials = 0; nTrials < pTrials; nTrials++) {
                // while submitting tasks, check whether it's already solved
                synchronized (lock) {
                    if (parallel_solved) {
                        es.shutdown(); // no more tasks accepted
                        break;
                    }
                }
                Callable<Boolean> c = createWorker(oracle, minimize, options.solverOpts.timeout, nTrials);
                Future<Boolean> f = ces.submit(c);
                futures.add(f);
            }
            // plog("=== submitted parallel trials: " + nTrials + " ===");
            // check tasks' results in the order of their completion
            for (int i = 0; i < nTrials; i++) {
                try {
                    Boolean r = ces.take().get((long) options.solverOpts.timeout, TimeUnit.MINUTES);
                    // whenever found a worker that finishes the job
                    if (r) {
                        plog("=== resolved within " + (i + 1) +
                                " complete parallel trial(s)");
                        worked = true;
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
            // terminate any alive CEGIS processes
            synchronized (lock) {
                for (Process p : cegiss) {
                    try {
                        p.exitValue();
                    } catch (IllegalThreadStateException e) {
                        p.destroy(); // if still running, kill the process
                    }
                }
            }
            // cancel any remaining tasks
            for (Future<Boolean> f : futures) {
                f.cancel(true);
            }
        }
        return worked;
    }

    @Override
    protected void beforeRunSolver(SynchronousTimedProcess proc) {
        // maintain CEGIS process, to terminate it directly
        // (when another worker already found a solution
        synchronized (lock) {
            cegiss.add(proc.getProc());
        }
    }

}
