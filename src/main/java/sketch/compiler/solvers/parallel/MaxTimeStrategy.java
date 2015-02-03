package sketch.compiler.solvers.parallel;

import sketch.compiler.main.cmdline.SketchOptions;

public class MaxTimeStrategy extends ATimeStrategy {

    public MaxTimeStrategy(SketchOptions options) {
        super(options, "[max(time)]");
    }

    @Override
    protected double estimator(double a, double b) {
        return Math.max(a, b);
    }

}
