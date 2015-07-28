package sketch.compiler.solvers.parallel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.inference.WilcoxonSignedRankTest;

import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.dataflow.recursionCtrl.RecursionControl;
import sketch.compiler.main.cmdline.SketchOptions;
import sketch.compiler.solvers.SATSolutionStatistics;
import sketch.compiler.solvers.constructs.ValueOracle;
import sketch.util.Pair;

public class WilcoxonStrategy extends ParallelBackend {

    final static String name = "[wilcoxon]";
    boolean hasMinimize;

    public WilcoxonStrategy(SketchOptions options, RecursionControl rcontrol,
            TempVarGen varGen)
    {
        super(options, rcontrol, varGen);
        tester = new WilcoxonSignedRankTest();
        tMap = new HashMap<Integer, List<Double>>();
        pMap = new HashMap<Integer, Mean>();
    }

    protected WilcoxonSignedRankTest tester;

    // mappings from degree to a list of elapsed times
    Map<Integer, List<Double>> tMap;
    // mappings from degree to (accumulated) propability
    Map<Integer, Mean> pMap;

    protected double[] computeDist(int degree) {
        if (!tMap.containsKey(degree)) {
            return new double[0];
        }
        List<Double> times = tMap.get(degree);
        double p = pMap.get(degree).getResult();
        double[] dist = new double[times.size()];
        for (int i = 0; i < times.size(); i++) {
            dist[i] = times.get(i) / p;
        }
        return dist;
    }

    // Implementing Fisher–Yates shuffle
    static void shuffleArray(double[] ar) {
        Random rnd = new Random();
        for (int i = ar.length - 1; i > 0; i--) {
            int index = rnd.nextInt(i + 1);
            // Simple swap
            double a = ar[index];
            ar[index] = ar[i];
            ar[i] = a;
        }
    }

    double[] truncate(double[] dist, int len) {
        if (dist.length <= len)
            return dist;
        double[] truncated = new double[len];
        shuffleArray(dist);
        for (int i = 0; i < len; i++) {
            truncated[i] = dist[i];
        }
        return truncated;
    }

    protected double computeMean(int degree) {
        if (!tMap.containsKey(degree)) {
            return 0;
        }
        List<Double> times = tMap.get(degree);
        double p = pMap.get(degree).getResult();
        Mean m = new Mean();
        for (Double t : times) {
            m.increment(t / p);
        }
        return m.getResult();
    }

    @SuppressWarnings("serial")
    class Lucky extends Exception {
        Lucky(String msg) {
            super(msg);
        }
    }

    protected void sample(int degree) throws Lucky {
        List<Double> dist_t;
        Mean p;
        if (tMap.containsKey(degree)) {
            dist_t = tMap.get(degree);
            p = pMap.get(degree);
        } else {
            dist_t = new ArrayList<Double>();
            tMap.put(degree, dist_t);
            p = new Mean();
            pMap.put(degree, p);
        }

        List<SATSolutionStatistics> stats = runSyncTrials(oracle, hasMinimize, degree);
        StringBuilder buf = new StringBuilder();
        buf.append(name + " degree " + degree + " sample: ");
        for (SATSolutionStatistics stat : stats) {
            if (stat.successful()) {
                throw new Lucky(name + " lucky (degree: " + degree + ")");
            }
            long t = stat.elapsedTimeMs();
            dist_t.add((double) t);
            p.increment(stat.probability);
            buf.append("(" + t + ", " + stat.probability + ") ");
        }
        buf.append("\n");
        buf.append(name + " degree " + degree + " probability: ");
        buf.append(p.getResult());
        plog(buf.toString());
    }

    int sampleBound = test_trial_max * 3;
    double pValue = 0.17;

    protected int compareDegree(int degree_a, int degree_b) throws Lucky {
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

            // need more samplings
            if (len_a <= len_b) {
                sample(degree_a);
            }
            if (len_a >= len_b) {
                sample(degree_b);
            }
        }
        int res;
        StringBuilder buf = new StringBuilder();
        buf.append(name + " compareDegree(" + degree_a + ", " + degree_b + ") = ");
        if (pvalue > pValue) { // can't tell which one is better
            res = 0;
        }
        else {
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

    int pow2(int x) {
        return (int) (Math.pow(2, x));
    }

    final static int initExp = 4;

    Pair<Integer, Integer> climb() throws Lucky {
        int prvexp = initExp;
        int exp = prvexp + 1;
        while (exp <= 12) {
            int degree_l = pow2(prvexp);
            int degree_h = pow2(exp);
            int c = compareDegree(degree_l, degree_h);
            if (c < 0) { // prvexp is better
                return new Pair<Integer, Integer>(prvexp, exp);
            } else if (c > 0) { // exp is better
                prvexp = exp;
                exp++;
            } else { // can't tell
                exp++;
            }
        }
        return new Pair<Integer, Integer>(prvexp, exp);
    }

    int binarySearch(int degree_l, int degree_h) throws Lucky {
        plog(name + " search [" + degree_l + " " + degree_h + "]");
        if (degree_h - degree_l <= pow2(initExp)) {
            double mean_l = computeMean(degree_l);
            double mean_h = computeMean(degree_h);
            if (mean_l <= mean_h)
                return degree_l;
            else
                return degree_h;
        } else {
            int degree_m = (degree_l + degree_h) / 2;
            int c = compareDegree(degree_l, degree_m);
            if (c < 0) { // low is better
                return binarySearch(degree_l, degree_m);
            } else if (c > 0) { // mid is better
                return binarySearch(degree_m, degree_h);
            } else { // can't tell
                return degree_m;
            }
        }
    }

    // do something in-between learning and testing phases
    protected void onStageChanged() {
    }

    @Override
    protected boolean solve(ValueOracle oracle, boolean hasMinimize, float timeoutMins) {
        this.hasMinimize = hasMinimize;
        int old_ntimes = options.solverOpts.ntimes;
        options.solverOpts.ntimes = 0;
        stage = STAGE.LEARNING;

        try {
            Pair<Integer, Integer> range = climb();
            int degree_l = pow2(range.getFirst());
            int degree_h = pow2(range.getSecond());
            int degree = binarySearch(degree_l, degree_h);
            plog(name + " degree choice: " + degree);
            options.solverOpts.randdegree = degree;
            randdegrees.clear();
        } catch (Lucky e) {
            plog(e.getMessage());
            return true;
        }

        onStageChanged();
        options.solverOpts.ntimes = old_ntimes;
        stage = STAGE.TESTING;
        return super.solve(oracle, hasMinimize, timeoutMins);
    }

}
