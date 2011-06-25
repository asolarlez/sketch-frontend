package sketch.compiler.smt.stp;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({LanguageBasicBlastBV.class, LanguageBasicFHBlastBV.class, LanguageBasicCanonFHBlastBV.class,
    LanguageBasicTOABV.class, LanguageBasicCanonFHTOABV.class})
public class LanguageBasic {
}
