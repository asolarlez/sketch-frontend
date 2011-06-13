package sketch.compiler.smt.yices2;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
    LanguageBasicBlastInt.class,
    LanguageBasicBlastBV.class, 
    LanguageBasicFHBlastBV.class, 
    LanguageBasicCanonFHBlastBV.class,
    LanguageBasicTOABV.class, 
    LanguageBasicCanonFHTOABV.class})
public class LanguageBasic {
}
