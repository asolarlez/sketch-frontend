package sketch.compiler.smt.cvc3;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.Result;

public class AllTests {

	
	public static void main(String[] args) {
		Result runClasses = org.junit.runner.JUnitCore.runClasses(LanguageBasicTOAInt.class);
		assertTrue("Failed count is " + runClasses.getFailureCount(),
				runClasses.getFailureCount() == 0);
	}
	
	@Test
	public void languageBasicTOAIntTests() {
		Result runClasses = org.junit.runner.JUnitCore.runClasses(LanguageBasicTOAInt.class);
		assertTrue("Failed count is " + runClasses.getFailureCount(),
				runClasses.getFailureCount() == 0);
	}
	@Test
	public void languageBasicBlastIntTests() {
		Result runClasses = org.junit.runner.JUnitCore.runClasses(LanguageBasicBlastInt.class);
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
	public void languageBasicBlastBVTests() {
		Result runClasses = org.junit.runner.JUnitCore.runClasses(LanguageBasicBlastBV.class);
		assertTrue("Failed count is " + runClasses.getFailureCount(),
				runClasses.getFailureCount() == 0);
	}
	
	@Test
	public void loopTOAIntTests() {
		Result runClasses = org.junit.runner.JUnitCore.runClasses(LoopTOAInt.class);
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
	public void loopBlastIntTests() {
		Result runClasses = org.junit.runner.JUnitCore.runClasses(LoopBlastInt.class);
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
	public void regularExprTOAIntTests() {
		Result runClasses = org.junit.runner.JUnitCore.runClasses(RegularExprTOAInt.class);
		assertTrue("Failed count is " + runClasses.getFailureCount(),
				runClasses.getFailureCount() == 0);
	}
	
	@Test
	public void regularExprTOABVTests() {
		Result runClasses = org.junit.runner.JUnitCore.runClasses(RegularExprTOABV.class);
		assertTrue("Failed count is " + runClasses.getFailureCount(),
				runClasses.getFailureCount() == 0);
	}
	
	@Test
	public void regularExprBlastIntTests() {
		Result runClasses = org.junit.runner.JUnitCore.runClasses(RegularExprBlastInt.class);
		assertTrue("Failed count is " + runClasses.getFailureCount(),
				runClasses.getFailureCount() == 0);
	}
	
	@Test
	public void regularExprBlastBVTests() {
		Result runClasses = org.junit.runner.JUnitCore.runClasses(RegularExprBlastBV.class);
		assertTrue("Failed count is " + runClasses.getFailureCount(),
				runClasses.getFailureCount() == 0);
	}

	
}
