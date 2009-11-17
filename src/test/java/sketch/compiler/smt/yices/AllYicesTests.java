package sketch.compiler.smt.yices;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({LanguageBasicBlastBV.class, SketchRegressionTests.class, BitVectorBlastBV.class})
public class AllYicesTests {
}
