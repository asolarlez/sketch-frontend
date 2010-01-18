package sketch.compiler.smt.stp;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses( {
        SketchShowcaseCSECanonFHBlastBV.class,
        SketchShowcaseCanonFHBlastBV.class,
        SketchShowcaseCanonFHTOABV.class,
        SketchShowcaseBlastBV.class, SketchShowcaseTOABV.class,
        SketchShowcaseFHBlastBV.class
         })
public class SketchShowcaseSuite {
}
