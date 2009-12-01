package sketch.compiler.smt.z3;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({SketchShowcaseBlastBV.class, SketchShowcaseTOABV.class, SketchShowcaseFHBlastBV.class, SketchShowcaseCanonFHBlastBV.class, SketchShowcaseCanonFHTOABV.class})
public class SketchShowcaseSuite {
}
