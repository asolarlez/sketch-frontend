package sketch.compiler.solvers.parallel;

import java.util.List;

import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.dataflow.recursionCtrl.RecursionControl;
import sketch.compiler.main.cmdline.SketchOptions;
import sketch.compiler.solvers.SATSolutionStatistics;
import sketch.compiler.solvers.constructs.ValueOracle;

public class StrategicalBackend extends ParallelBackend {

    IStrategy strategy = null;
    ParallelBackend proxy = null;

    public StrategicalBackend(SketchOptions options, RecursionControl rcontrol,
            TempVarGen varGen)
    {
        super(options, rcontrol, varGen);
        switch (options.solverOpts.strategy) {
            case NOT_SET:
                break;
            case MIN_TIME:
                strategy = new MinTimeStrategy(options);
                break;
            case MAX_TIME:
                strategy = new MaxTimeStrategy(options);
                break;
            case WILCOXON:
                // proxy = new WilcoxonStrategy(options, rcontrol, varGen);
                proxy = new AsyncWilcoxonStrategy(options, rcontrol, varGen);
                break;
        }
    }

    @Override
    protected boolean solve(ValueOracle oracle, boolean hasMinimize, float timeoutMins) {
        // if the strategy requires the whole control, rather than an iterative manner,
        // pass the control to that strategy as a proxy
        if (proxy != null) {
            return proxy.solve(oracle, hasMinimize, timeoutMins);
        }

        if (strategy != null) {
            int old_ntimes = options.solverOpts.ntimes;
            options.solverOpts.ntimes = 0;
            stage = STAGE.LEARNING;
            plog(strategy.getName() + " degree searching...");

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
                        runSyncTrials(oracle, hasMinimize, next_d);
                // check if we're too lucky: found a solution while test runs
                for (SATSolutionStatistics stat : results) {
                    if (stat.successful()) {
                        plog(strategy.getName() + " lucky (degree: " + next_d + ")");
                        return true;
                    }
                }
                // o.w., feed the results and keep iteration
                strategy.pushInfo(next_d, results);
            }
            int d = strategy.getDegree();
            plog(strategy.getName() + " degree choice: " + d);
            options.solverOpts.randdegree = d;
            randdegrees.clear();

            options.solverOpts.ntimes = old_ntimes;
            stage = STAGE.TESTING;
        }
        return super.solve(oracle, hasMinimize, timeoutMins);
    }

}
