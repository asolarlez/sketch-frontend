package sketch.compiler.solvers.parallel;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.inference.WilcoxonSignedRankTest;

import sketch.compiler.main.cmdline.SketchOptions;
import sketch.compiler.solvers.SATSolutionStatistics;

public class WilcoxonStrategy implements IStrategy {

    final static String name = "Wilcoxon";
    SketchOptions options;
    List<Integer> degrees;

    int degree_l;
    int degree_r;

    double[] dist_l = null;
    double[] dist_r = null;

    public WilcoxonStrategy(SketchOptions options) {
        this.options = options;
        degrees = new ArrayList<Integer>();
        degree_l = 100;
        degree_r = 6400;
        degrees.add(degree_l);
        degrees.add(degree_r);
    }

    public String getName() {
        return name;
    }

    public boolean hasNextDegree() {
        return !degrees.isEmpty() && degree_l < degree_r;
    }

    public int nextDegreeToTry() {
        if (!degrees.isEmpty()) {
            return degrees.remove(0);
        } else {
            return -1;
        }
    }

    public void pushInfo(int degree, List<SATSolutionStatistics> trials) {
        if (degree != degree_l && degree != degree_r)
            return;

        // calculate distribution: time / p
        // estimated p: 1 / search space, assuming there is only one solution
        // so, distribution: time * mean(search space)
        double[] dist = new double[trials.size()];
        Mean sspace = new Mean();
        StringBuilder buf = new StringBuilder();
        buf.append(getName() + " pushed info: degree " + degree + System.lineSeparator());
        for (int i = 0; i < dist.length; i++) {
            SATSolutionStatistics stat = trials.get(i);
            dist[i] = stat.elapsedTimeMs();
            sspace.increment(stat.searchSpace);
        }
        buf.append(getName() + " mean(search space): " + sspace.getResult());
        buf.append(System.lineSeparator());
        for (int i = 0; i < dist.length; i++) {
            dist[i] *= sspace.getResult();
            buf.append(dist[i] + " ");
        }
        if (options.debugOpts.verbosity >= 5) {
            System.out.println(buf.toString());
        }

        // update distribution
        if (degree == degree_l) {
            dist_l = dist;
        } else if (degree == degree_r) {
            dist_r = dist;
        }

        if (dist_l == null || dist_r == null)
            return;

        WilcoxonSignedRankTest tester = new WilcoxonSignedRankTest();
        double pvalue = tester.wilcoxonSignedRankTest(dist_l, dist_r, false);
        if (options.debugOpts.verbosity >= 5) {
            System.out.println(getName() + " test " + "(" + degree_l + ", " + degree_r + "): " + pvalue);
        }
        if (pvalue < 0.1) { // the median difference is significant
            Mean dist_mean = new Mean();
            for (int i = 0; i < dist_l.length; i++) {
                dist_mean.increment(dist_l[i]);
            }
            double dist_l_mean = dist_mean.getResult();
            dist_mean.clear();
            for (int i = 0; i < dist_r.length; i++) {
                dist_mean.increment(dist_r[i]);
            }
            double dist_r_mean = dist_mean.getResult();
            if (dist_l_mean < dist_r_mean) { // degree_l is better
                int next_r = degree_r / 2;
                if (degree_l < next_r) {
                    degree_r = next_r;
                    degrees.add(next_r);
                } else if (next_r <= degree_l) { // pivot x-ing
                    degree = degree_l;
                }
            } else { // degree_r is better
                int next_l = degree_l * 2;
                if (next_l < degree_r) {
                    degree_l = next_l;
                    degrees.add(next_l);
                } else if (degree_r <= next_l) { // pivot x-ing
                    degree = degree_r;
                }
            }
        }
    }

    int degree = -1;

    public int getDegree() {
        if (degree > 0) // already computed
            return degree;

        if (dist_l == null || dist_r == null) {
            System.err.println(getName() + "cannot predict a degree");
            return 100;
        }

        Mean dist_mean = new Mean();
        for (int i = 0; i < dist_l.length; i++) {
            dist_mean.increment(dist_l[i]);
        }
        double dist_l_mean = dist_mean.getResult();
        dist_mean.clear();
        for (int i = 0; i < dist_r.length; i++) {
            dist_mean.increment(dist_r[i]);
        }
        double dist_r_mean = dist_mean.getResult();

        if (dist_l_mean < dist_r_mean) {
            degree = degree_l;
        } else {
            degree = degree_r;
        }

        return degree;
    }

}
