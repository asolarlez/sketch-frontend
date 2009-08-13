/**
 * 
 */
package sketch.compiler.test;

import sketch.compiler.main.sten.StencilSketchMain;

public class StencilSketchRunner extends SketchRunner {
    public StencilSketchRunner(String path) throws Exception {
        super(path);
    }

    @Override
    protected void runSketch(String[] args) {
        StencilSketchMain.main(args);
    }
}
