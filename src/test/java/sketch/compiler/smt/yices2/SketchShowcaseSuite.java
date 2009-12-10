package sketch.compiler.smt.yices2;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
    SketchShowcaseCanonFHBlastBV.class,
    SketchShowcaseCanonFHTOABV.class,
    SketchShowcaseBlastBV.class, 
    SketchShowcaseTOABV.class, 
    SketchShowcaseFHBlastBV.class
    })
public class SketchShowcaseSuite {
}
