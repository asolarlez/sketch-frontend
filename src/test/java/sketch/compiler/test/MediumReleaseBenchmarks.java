package sketch.compiler.test;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import sketch.compiler.main.seq.SequentialSketchMain;

/**
 * Run tests src/release_benchmarks/sk/medium/*.sk
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
public class MediumReleaseBenchmarks extends SketchTestSuite {
    public MediumReleaseBenchmarks() throws Exception {
        super(BasicFileFilter.sketches("src", "release_benchmarks", "sk", "medium"));
        SequentialSketchMain.isTest = true;
    }

    public static TestSuite suite() throws Exception {
        return new MediumReleaseBenchmarks();
    }

    @Override
    public TestCase getTestInstance(String path) throws Exception {
        return new SequentialSketchRunner(path);
    }
}
