package sketch.compiler.smt.yices2;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;


@RunWith(Suite.class)
@SuiteClasses({LanguageBasicBlastBV.class, LanguageBasicTOABV.class, SketchRegressionTests.class, BitVectorBlastBV.class})
public class AllYices2Tests {
}
