package sketch.compiler.solvers.parallel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import sketch.compiler.main.cmdline.SketchOptions;
import sketch.compiler.solvers.SATSolutionStatistics;

public abstract class ATimeStrategy implements IStrategy {

    SketchOptions options;
    String name;
    List<Integer> degrees;
    Map<Integer, Double> avgTimes;

    public ATimeStrategy(SketchOptions options, String name) {
        this.options = options;
        this.name = name;
        degrees = new ArrayList<Integer>();
        degrees.addAll(Arrays.asList(100, 400, 1600, 6400));
        avgTimes = new TreeMap<Integer, Double>();
    }

    public String getName() {
        return name;
    }

    public boolean hasNextDegree() {
        return degrees != null && !degrees.isEmpty();
    }

    public int nextDegreeToTry() {
        return degrees.remove(0);
    }

    public void pushInfo(int degree, List<SATSolutionStatistics> trials) {
        if (trials == null || trials.isEmpty())
            return;

        StringBuilder buf = new StringBuilder();
        buf.append(getName() + " pushed info: degree " + degree + " ");
        double sum = 0;
        for (SATSolutionStatistics stat : trials) {
            sum += stat.elapsedTimeMs();
            buf.append(stat.elapsedTimeMs());
            buf.append(' ');
        }
        if (options.debugOpts.verbosity >= 5) {
            System.out.println(buf.toString());
        }
        avgTimes.put(degree, sum / trials.size());
    }

    abstract protected double estimator(double a, double b);

    int degree = -1;

    public int getDegree() {
        if (degree > 0) // already computed
            return degree;
        if (avgTimes.isEmpty()) {
            System.err.println(getName() + " cannot predict a degree");
            return 100;
        }

        double best = -1;
        Iterator<Entry<Integer, Double>> it = avgTimes.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Double> pair = (Map.Entry<Integer, Double>) it.next();
            if (best < 0 || best != estimator(best, pair.getValue())) {
                degree = pair.getKey();
                best = pair.getValue();
                if (options.debugOpts.verbosity >= 5) {
                    System.out.println(getName() + " " + degree + " (" + best + ")");
                }
            }
        }
        return degree;
    }
}
