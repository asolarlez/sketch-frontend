package sketch.compiler.smt.yices2;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({SketchShowcaseBlastBV.class, 
     
    SketchShowcaseFHBlastBV.class, 
    SketchShowcaseCanonFHBlastBV.class,
    SketchShowcaseCanonFHTOABV.class,
    SketchShowcaseTOABV.class
    })
public class SketchShowcaseSuite {
}
