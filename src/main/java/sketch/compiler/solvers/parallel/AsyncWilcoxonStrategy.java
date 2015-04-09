package sketch.compiler.solvers.parallel;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.math3.stat.descriptive.moment.Mean;

import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.dataflow.recursionCtrl.RecursionControl;
import sketch.compiler.main.cmdline.SketchOptions;
import sketch.compiler.solvers.SATSolutionStatistics;
import sketch.compiler.solvers.constructs.ValueOracle;

public class AsyncWilcoxonStrategy extends WilcoxonStrategy implements
        IAsyncManager<SATSolutionStatistics>
{
    public SATSolutionStatistics callSolver(ValueOracle oracle, boolean hasMinimize,
            float timeoutMins, int fileIdx)
    {
        return super.incrementalSolve(oracle, hasMinimize, timeoutMins, fileIdx);
    }

    public void plog(String msg) {
        super.plog(msg);
    }

    public void plog(PrintStream out, String msg) {
        super.plog(out, msg);
    }

    public SketchOptions getOptions() {
        return options;
    }

    // exception/message about solution finding
    Lucky lucky;

    public void found(int degree) {
        synchronized (lock) {
            parallel_solved = true;
            lucky = new Lucky(name + " lucky (degree: " + degree + ")");
        }
        // wake up the manager thread if waiting
        synchronized (managerLock) {
            managerLock.notify();
        }
        // clean up the thread pool
        cleanUpPool();
    }

    public boolean aborted() {
        synchronized (lock) {
            return parallel_solved;
        }
    }

    public AsyncWilcoxonStrategy(SketchOptions options, RecursionControl rcontrol,
            TempVarGen varGen)
    {
        super(options, rcontrol, varGen);
        managerLock = new Object();
        dMap = new HashMap<Integer, List<Double>>();
        sMap = new HashMap<Integer, Integer>();
    }

    // mappings from degree to distribution (= t/p)
    Map<Integer, List<Double>> dMap;
    // mappings from degree to # of sampling request
    Map<Integer, Integer> sMap;

    @Override
    protected double[] computeDist(int degree) {
        if (!dMap.containsKey(degree)) {
            return new double[0];
        }
        List<Double> dist_tp = dMap.get(degree);
        double[] dist = new double[dist_tp.size()];
        for (int i = 0; i < dist_tp.size(); i++) {
            dist[i] = dist_tp.get(i);
        }
        return dist;
    }

    @Override
    protected double computeMean(int degree) {
        if (!dMap.containsKey(degree)) {
            return 0;
        }
        List<Double> dist_tp = dMap.get(degree);
        Mean m = new Mean();
        for (Double tp : dist_tp) {
            m.increment(tp);
        }
        return m.getResult();
    }

    // call-back when one back-end task has been done
    public void notifyWorkerDone(SATSolutionStatistics stat, int degree) {
        // update statistics without regard to active degrees
        List<Double> dist_tp;
        if (dMap.containsKey(degree)) {
            dist_tp = dMap.get(degree);
        } else {
            dist_tp = new ArrayList<Double>();
            dMap.put(degree, dist_tp);
        }
        long t = stat.elapsedTimeMs();
        dist_tp.add((double) t / stat.probability);

        // check whether this output came from the degree we're looking for
        boolean is_active = false;
        synchronized (lock) {
            is_active = degree == active_degree_1 || degree == active_degree_2;
        }

        // if the current degree is what we're actively sampling
        if (is_active) {
            checkSampledEnough();
        }
    }

    // run trials *asynchronously*
    // i.e., this method will yield the control after forking workers
    void runAsyncTrials(ValueOracle oracle, boolean hasMinimize, int d, int n) {
        int nTrial;
        for (nTrial = 0; nTrial < n; nTrial++) {
            // while submitting tasks, check whether it's already solved
            synchronized (lock) {
                if (parallel_solved) {
                    es.shutdown(); // no more tasks accepted
                    break;
                }
            }
            AsyncWorker worker =
                    new AsyncWorker(this, oracle, hasMinimize, timeoutMins, nTrial, d);
            Future<SATSolutionStatistics> f = ces.submit(worker);
            futures.add(f);
        }
    }

    @Override
    protected void sample(int degree) {
        recordSampleRequest(degree, test_trial_max);
        // simply start asynchronous trials
        plog(name + " start sampling: degree " + degree);
        runAsyncTrials(oracle, hasMinimize, degree, test_trial_max);
        // statistics will be updated at the call-back method
    }

    // how many times sampling requests have been made for this degree
    int sampleRequested(int degree) {
        if (sMap.containsKey(degree)) {
            return sMap.get(degree);
        } else {
            sMap.put(degree, 0);
            return 0;
        }
    }

    void recordSampleRequest(int degree, int n) {
        int prev = sampleRequested(degree);
        sMap.put(degree, prev + n);
    }

    private Object managerLock;
    private boolean sampledEnough = false;

    int active_degree_1 = 0;
    int active_degree_2 = 0;

    // how many samples should we have at least?
    int test_trial_min = (int) (test_trial_max * 0.8);
    // how many differences can we allow?
    static final int tolerable_gap = 2;

    // when called back, check whether samplings are good enough
    void checkSampledEnough() {
        List<Double> dist_tp1;
        List<Double> dist_tp2;
        synchronized (lock) {
            // if either active degree is still empty, then wait
            if (!dMap.containsKey(active_degree_1))
                return;
            if (!dMap.containsKey(active_degree_2))
                return;
            dist_tp1 = dMap.get(active_degree_1);
            dist_tp2 = dMap.get(active_degree_2);
        }

        int len1 = dist_tp1.size();
        int len2 = dist_tp2.size();

        // whether active degrees' sample size reaches the minimum
        if (len1 < test_trial_min)
            return;
        if (len2 < test_trial_min)
            return;

        // whether the gap of active degrees' sampling size is tolerable
        int gap = Math.abs(len1 - len2) % test_trial_max;
        if (gap > tolerable_gap)
            return;

        // wake up the manager thread
        synchronized (managerLock) {
            plog(name + " waking up the manager: (d1: " + active_degree_1 + ", len=" +
                    len1 + ") (d2: " + active_degree_2 + ", len=" + len2 + ")");
            sampledEnough = true;
            managerLock.notify();
        }
    }

    // the manager thread will wait here
    // until samples are good enough to proceed
    void waitUntilSampledEnough() throws Lucky {
        synchronized (managerLock) {
            // to not miss the signal and not wake up for spurious cases
            while (!sampledEnough && !parallel_solved) {
                try {
                    managerLock.wait();
                } catch (InterruptedException e) {}
            }
            // clear signal
            sampledEnough = false;
        }
        // double-check whether a solution was found
        synchronized (lock) {
            if (parallel_solved) {
                throw lucky;
            }
        }
    }

    @Override
    protected int compareDegree(int degree_a, int degree_b) throws Lucky {
        synchronized (lock) {
            active_degree_1 = degree_a;
            active_degree_2 = degree_b;
        }
        int req_a = 0;
        int req_b = 0;

        int len_a = 0;
        int len_b = 0;
        double pvalue = 1;
        while (len_a <= sampleBound && len_b <= sampleBound) {
            double[] dist_a = computeDist(degree_a);
            double[] dist_b = computeDist(degree_b);
            len_a = dist_a.length;
            len_b = dist_b.length;

            // adjust dist size as Wilcoxon test requires them to be same
            if (len_a != len_b) {
                int len = Math.min(len_a, len_b);
                if (len_a > len) {
                    dist_a = truncate(dist_a, len);
                }
                if (len_b > len) {
                    dist_b = truncate(dist_b, len);
                }
            }
            if (len_a > 0 && len_b > 0) {
                plog(name + " degree " + degree_a + " dist: " + Arrays.toString(dist_a));
                plog(name + " degree " + degree_b + " dist: " + Arrays.toString(dist_b));
                pvalue = tester.wilcoxonSignedRankTest(dist_a, dist_b, false);
                plog(name + " test (" + degree_a + ", " + degree_b + "): " + pvalue);
            }
            if (pvalue <= pValue) // confident enough
                break;
            if (len_a >= sampleBound && len_b >= sampleBound) // too many samples
                break;

            req_a = sampleRequested(degree_a);
            req_b = sampleRequested(degree_b);
            if (req_a >= sampleBound && req_b >= sampleBound) // too many samples
                break;
            if (req_a <= req_b && req_a < sampleBound) {
                sample(degree_a);
            }
            if (req_a >= req_b && req_b < sampleBound) {
                sample(degree_b);
            }
            waitUntilSampledEnough();
        }
        int res;
        StringBuilder buf = new StringBuilder();
        buf.append(name + " compareDegree(" + degree_a + ", " + degree_b + ") = ");
        if (pvalue > pValue) { // can't tell which one is better
            res = 0;
        } else {
            double mean_a = computeMean(degree_a);
            double mean_b = computeMean(degree_b);
            if (mean_a < mean_b)
                res = -1;
            else
                res = 1;
        }
        buf.append(res);
        plog(buf.toString());
        return res;
    }

    float timeoutMins;

    ExecutorService es;
    CompletionService<SATSolutionStatistics> ces;
    List<Future<SATSolutionStatistics>> futures;

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

    @Override
    protected void onStageChanged() {
        // clean up the thread pool
        cleanUpPool();
        // from now on, i.e., at the testing phase,
        // we will use ParallelBackend's thread pool instead
    }

    @Override
    protected boolean solve(ValueOracle oracle, boolean hasMinimize, float timeoutMins) {
        this.timeoutMins = timeoutMins;

        // generate worker pool and managed executor
        es = Executors.newFixedThreadPool(cpu);
        ces = new ExecutorCompletionService<SATSolutionStatistics>(es);
        // place to maintain future parallel tasks
        futures = new ArrayList<Future<SATSolutionStatistics>>(test_trial_max);

        return super.solve(oracle, hasMinimize, timeoutMins);
    }
}
