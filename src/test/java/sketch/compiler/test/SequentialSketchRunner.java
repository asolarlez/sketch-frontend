/**
 * 
 */
package sketch.compiler.test;

import sketch.compiler.main.seq.SequentialSketchMain;

public class SequentialSketchRunner extends SketchRunner {
    public SequentialSketchRunner(String path) throws Exception {
        super(path);
    }

    @Override
    protected void runSketch(String[] args) {
        SequentialSketchMain.main(args);
    }
}
