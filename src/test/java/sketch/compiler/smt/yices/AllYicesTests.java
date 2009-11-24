package sketch.compiler.smt.yices;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({LanguageBasicBlastBV.class, SketchRegressionTests.class, BitVectorBlastBV.class})
// LanguageBasicTOABV.class is not included because for most tests, yices doesn't
// show any model. weird.
public class AllYicesTests {
}
