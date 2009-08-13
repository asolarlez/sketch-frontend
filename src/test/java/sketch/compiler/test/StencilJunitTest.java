package sketch.compiler.test;

import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Run all tests in src/test/sk/seq/miniTest*.sk
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you
 *          make changes, please consider contributing back!
 */
public class StencilJunitTest extends SketchTestSuite {
    public StencilJunitTest() throws Exception {
        super("sten");
    }

    public static TestSuite suite() throws Exception {
        return new StencilJunitTest();
    }

    @Override
    public TestCase getTestInstance(String path) throws Exception {
        return new StencilSketchRunner(path);
    }
}
