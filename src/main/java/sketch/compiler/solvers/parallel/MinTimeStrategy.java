package sketch.compiler.solvers.parallel;

import sketch.compiler.main.cmdline.SketchOptions;

public class MinTimeStrategy extends ATimeStrategy {

    public MinTimeStrategy(SketchOptions options) {
        super(options, "min(time)");
    }

    @Override
    protected double estimator(double a, double b) {
        return Math.min(a, b);
    }

}
