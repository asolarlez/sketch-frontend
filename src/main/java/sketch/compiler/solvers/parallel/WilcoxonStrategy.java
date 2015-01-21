package sketch.compiler.solvers.parallel;

import java.util.List;

import sketch.compiler.main.cmdline.SketchOptions;
import sketch.compiler.solvers.SATSolutionStatistics;

public class WilcoxonStrategy implements IStrategy {

    final static String name = "Wilcoxon";
    SketchOptions options;

    public WilcoxonStrategy(SketchOptions options) {
        this.options = options;
    }

    public String getName() {
        return name;
    }

    public boolean hasNextDegree() {
        // TODO Auto-generated method stub
        return false;
    }

    public int nextDegreeToTry() {
        // TODO Auto-generated method stub
        return 0;
    }

    public void pushInfo(int degree, List<SATSolutionStatistics> trials) {
        // TODO Auto-generated method stub
    }

    public int getDegree() {
        // TODO Auto-generated method stub
        return 0;
    }

}
