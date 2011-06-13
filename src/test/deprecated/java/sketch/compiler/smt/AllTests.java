package sketch.compiler.smt;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import sketch.compiler.smt.stp.AllSTPTests;
import sketch.compiler.smt.yices.AllYicesTests;

@RunWith(Suite.class)
@SuiteClasses({AllSTPTests.class, AllYicesTests.class})
public class AllTests {
}
