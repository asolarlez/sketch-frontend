package sketch.compiler.solvers.parallel;

import java.util.List;

import sketch.compiler.solvers.SATSolutionStatistics;

public interface IStrategy {
    // debug purpose
    public String getName();

    // whether the strategy executor needs to try another degree
    public boolean hasNextDegree();

    // what degree the strategy executor should try?
    public int nextDegreeToTry();

    // push information about trials back to strategy
    public void pushInfo(int degree, List<SATSolutionStatistics> trials);

    // degree chosen by the strategy
    public int getDegree();
}
