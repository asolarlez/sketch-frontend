package sketch.compiler.test;

import junit.framework.TestSuite;

/**
 * run a subset of tests from other test suites
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you
 *          make changes, please consider contributing back!
 */
public class QuickTest extends TestSuite {
    public QuickTest(TestSuite[] suites, int skipEvery) {
        int ctr = 0;
        for (TestSuite suite : suites) {
            for (int a = 0; a < suite.countTestCases(); a++) {
                if (ctr % skipEvery == 0) {
                    addTest(suite.testAt(a));
                }
                ctr += 1;
            }
        }
    }

    public static TestSuite suite() throws Exception {
        TestSuite[] suites = { new SequentialJunitTest() };
        return new QuickTest(suites, 16);
    }
}
