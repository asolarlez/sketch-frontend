package sketch.compiler.smt.stp;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({SketchRegressionBlastBV.class, SketchRegressionTOABV.class, SketchRegressionFHBlastBV.class, SketchRegressionCanonFHBlastBV.class, SketchRegressionCanonFHTOABV.class})
public class SketchRegressionSuite {
}
