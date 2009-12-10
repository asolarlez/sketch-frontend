package sketch.compiler.smt.z3;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
    SketchShowcaseCSECanonFHBlastBV.class,
    SketchShowcaseCanonFHBlastBV.class,
    SketchShowcaseBlastBV.class, 
    SketchShowcaseTOABV.class, 
    SketchShowcaseFHBlastBV.class, 
    SketchShowcaseCanonFHTOABV.class,})
public class SketchShowcaseSuite {
}
