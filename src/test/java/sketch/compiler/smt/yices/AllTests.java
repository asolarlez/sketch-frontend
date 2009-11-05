package sketch.compiler.smt.yices;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.Result;

public class AllTests {

	public static void main(String[] args) {
		Result runClasses = org.junit.runner.JUnitCore.runClasses(LanguageBasicBlastBV.class);
		assertTrue("Failed count is " + runClasses.getFailureCount(),
				runClasses.getFailureCount() == 0);
	}
	
	@Test
	public void languageBasicBlastBVTests() {
		Result runClasses = org.junit.runner.JUnitCore.runClasses(LanguageBasicBlastBV.class);
		assertTrue("Failed count is " + runClasses.getFailureCount(),
				runClasses.getFailureCount() == 0);
	}
	
	@Test
	public void languageBasicTOABVTests() {
		Result runClasses = org.junit.runner.JUnitCore.runClasses(LanguageBasicTOABV.class);
		assertTrue("Failed count is " + runClasses.getFailureCount(),
				runClasses.getFailureCount() == 0);
	}
	
	@Test
	public void loopBlastBVTests() {
		Result runClasses = org.junit.runner.JUnitCore.runClasses(LoopBlastBV.class);
		assertTrue("Failed count is " + runClasses.getFailureCount(),
				runClasses.getFailureCount() == 0);
	}
	
	@Test
	public void loopTOABVTests() {
		Result runClasses = org.junit.runner.JUnitCore.runClasses(LoopTOABV.class);
		assertTrue("Failed count is " + runClasses.getFailureCount(),
				runClasses.getFailureCount() == 0);
	}
	
	@Test
	public void regularExprBlastBVTests() {
		Result runClasses = org.junit.runner.JUnitCore.runClasses(RegularExprBlastBV.class);
		assertTrue("Failed count is " + runClasses.getFailureCount(),
				runClasses.getFailureCount() == 0);
	}
	
	@Test
	public void regularExprTOABVTests() {
		Result runClasses = org.junit.runner.JUnitCore.runClasses(RegularExprTOABV.class);
		assertTrue("Failed count is " + runClasses.getFailureCount(),
				runClasses.getFailureCount() == 0);
	}
	
	protected String[] initCmdArgs(String inputPath) {
		String[] args = {"--smtpath", System.getenv("smtpath"),
				"--backend", "yices",
				"--bv",
				"--arrayOOBPolicy", "assertions",
				"--heapsize", "10",
				"--keeptmpfiles",
				"--tmpdir", "/tmp/sketch",
				"--verbosity", "0",
		"--outputdir", "output/",
		inputPath,
//		"--trace",
//		"--fakesolver",
		};
		
		return args;
	}
	
}
