package sketch.compiler.solvers.parallel;

import java.util.List;

import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.dataflow.recursionCtrl.RecursionControl;
import sketch.compiler.main.cmdline.SketchOptions;
import sketch.compiler.solvers.SATSolutionStatistics;
import sketch.compiler.solvers.constructs.ValueOracle;
import sketch.util.Misc;

public class StrategicalBackend extends ParallelBackend {

    enum STAGE {
        LEARNING, TESTING
    };

    STAGE stage;

    IStrategy strategy;

    public StrategicalBackend(SketchOptions options, RecursionControl rcontrol,
            TempVarGen varGen)
    {
        super(options, rcontrol, varGen);
        switch (options.solverOpts.strategy) {
            case NOT_SET:
                strategy = null;
                break;
            case MIN_TIME:
                strategy = new MinTimeStrategy(options);
                break;
            case MAX_TIME:
                strategy = new MaxTimeStrategy(options);
                break;
            case WILCOXON:
                strategy = new WilcoxonStrategy(options);
                break;
        }
        stage = STAGE.LEARNING;
    }

    final static float test_timeout = 1; // 1min
    final static int test_trial_max = 20;

    List<SATSolutionStatistics> runTrials(ValueOracle oracle, boolean hasMinimize, int d)
    {
        options.solverOpts.randdegree = d;

        int old_v = options.debugOpts.verbosity;
        if (old_v < 5) {
            options.debugOpts.verbosity = 5; // to see hole concretization info
        }

        List<SATSolutionStatistics> stats =
                parallel_solve(oracle, hasMinimize, test_timeout, test_trial_max);

        options.debugOpts.verbosity = old_v;
        return stats;
    }

    @Override
    protected boolean solve(ValueOracle oracle, boolean hasMinimize, float timeoutMins) {
        if (strategy != null) {
            // until the strategy has a fixed degree
            while (strategy.hasNextDegree()) {
                // ask it what degree to test next
                int next_d = strategy.nextDegreeToTry();
                if (next_d < 0) {
                    plog(strategy.getName() + " tries a strange degree: " + next_d);
                    break;
                }
                // test that degree
                List<SATSolutionStatistics> results =
                        runTrials(oracle, hasMinimize, next_d);
                // check if we're too lucky: found a solution while test runs
                for (SATSolutionStatistics stat : results) {
                    if (stat.successful()) {
                        return true;
                    }
                }
                // o.w., feed the results and keep iteration
                strategy.pushInfo(next_d, results);
            }
            int d = strategy.getDegree();
            plog(strategy.getName() + " degree choice: " + d);
            options.solverOpts.randdegree = d;
        }
        stage = STAGE.TESTING;
        return super.solve(oracle, hasMinimize, timeoutMins);
    }

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
                    String hole = res.get(i);
                    int bound = Integer.parseInt(res.get(i + 1));
                    if (bound <= 1) continue;
                    stat.probability /= bound;
                }
            }
        }
        return stat;
    }
}
