/**
 * 
 */
package sketch.compiler.test;

import sketch.compiler.main.par.ParallelSketchMain;

public class ParallelSketchRunner extends SketchRunner {
    public ParallelSketchRunner(String path) throws Exception {
        super(path);
    }

    @Override
    protected void runSketch(String[] args) {
        ParallelSketchMain.main(args);
    }
}
