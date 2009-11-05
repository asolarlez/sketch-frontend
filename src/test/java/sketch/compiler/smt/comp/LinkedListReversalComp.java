package sketch.compiler.smt.comp;

import java.util.HashMap;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import sketch.compiler.CommandLineParamManager;

public class LinkedListReversalComp extends Comparison {

	@Override
	protected HashMap<String, String> initCmdArgs(String inputPath, String backend,
			String smtpath, boolean toa, boolean bv) {
		HashMap<String, String> argsMap = new HashMap<String, String>();
		
		argsMap.put("--smtpath", smtpath);
		argsMap.put("--backend", backend);
		
		if (toa)
			argsMap.put("--theoryofarray", null);
		if (bv) 
			argsMap.put("--bv", null);
		
		argsMap.put("--arrayOOBPolicy", "assertions");
		
		argsMap.put("--heapsize", "10");
		argsMap.put("--inbits", "32");
		argsMap.put("--intbits", "5");
		argsMap.put("--unrollamnt", "4");
		
		argsMap.put("--outputdir", "output/");
		argsMap.put("--tmpdir", tmpDirStr);
		argsMap.put("--keeptmpfiles", null);
		
		argsMap.put("--verbosity", "0");
//		argsMap.put("--showphase", "lowering");
//		argsMap.put("--trace", null);
		argsMap.put(inputPath, null);
		
		return argsMap;
	}

	
	@Before
	public void init() {
		if (!tmpDir.exists())
			tmpDir.mkdir();
	}

	@After
	public void cleanup() {
		CommandLineParamManager.getParams().clear();
	}

//	@Test
//	public void test1cvc3TOABV() throws Exception {
//		runTest(
//				"TOA BV",
//				"inputs/comparisons/LinkedListReversal/LinkedListReversal_ForSketch1.sk",
//				"cvc3smtlib", true, true);
//	}

//	@Test
//	public void test1cvc3TOAInt() throws Exception {
//		runTest(
//				"TOA Int",
//				"inputs/comparisons/LinkedListReversal/LinkedListReversal_ForSketch1.sk",
//				"cvc3smtlib", true, false);
//	}

	@Test
	public void test1cvc3BlastBV() throws Exception {
		runToSmt(
				"Blast BV",
				"inputs/comparisons/LinkedListReversal/LinkedListReversal_ForSketch1.sk",
				"cvc3smtlib", false, true);
	}

	@Test
	public void test1cvc3BlastInt() throws Exception {
		runToSmt(
				"Blast Int",
				"inputs/comparisons/LinkedListReversal/LinkedListReversal_ForSketch1.sk",
				"cvc3smtlib", false, false);
	}

//	@Test
//	public void test2cvc3TOABV() throws Exception {
//		runTest(
//				"TOA BV",
//				"inputs/comparisons/LinkedListReversal/LinkedListReversal_ForSketch2.sk",
//				"cvc3smtlib", true, true);
//	}

//	@Test
//	public void test2cvc3TOAInt() throws Exception {
//		runTest(
//				"TOA Int",
//				"inputs/comparisons/LinkedListReversal/LinkedListReversal_ForSketch2.sk",
//				"cvc3smtlib", true, false);
//	}

	@Test
	public void test2cvc3BlastBV() throws Exception {
		runToSmt(
				"Blast BV",
				"inputs/comparisons/LinkedListReversal/LinkedListReversal_ForSketch2.sk",
				"cvc3smtlib", false, true);
	}

	@Test
	public void test2cvc3BlastInt() throws Exception {
		runToSmt(
				"Blast Int",
				"inputs/comparisons/LinkedListReversal/LinkedListReversal_ForSketch2.sk",
				"cvc3smtlib", false, false);
	}

//	@Test
//	public void test3cvc3TOABV() throws Exception {
//		runTest(
//				"TOA BV",
//				"inputs/comparisons/LinkedListReversal/LinkedListReversal_ForSketch3.sk",
//				"cvc3smtlib", true, true);
//	}
//
//	@Test
//	public void test3cvc3TOAInt() throws Exception {
//		runTest(
//				"TOA Int",
//				"inputs/comparisons/LinkedListReversal/LinkedListReversal_ForSketch3.sk",
//				"cvc3smtlib", true, false);
//	}

	@Test
	public void test3cvc3BlastBV() throws Exception {
		runToSmt(
				"Blast BV",
				"inputs/comparisons/LinkedListReversal/LinkedListReversal_ForSketch3.sk",
				"cvc3smtlib", false, true);
	}

	@Test
	public void test3cvc3BlastInt() throws Exception {
		runToSmt(
				"Blast Int",
				"inputs/comparisons/LinkedListReversal/LinkedListReversal_ForSketch3.sk",
				"cvc3smtlib", false, false);
	}

//	@Test
//	public void test4cvc3TOABV() throws Exception {
//		runTest(
//				"TOA BV",
//				"inputs/comparisons/LinkedListReversal/LinkedListReversal_ForSketch4.sk",
//				"cvc3smtlib", true, true);
//	}
//
//	@Test
//	public void test4cvc3TOAInt() throws Exception {
//		runTest(
//				"TOA Int",
//				"inputs/comparisons/LinkedListReversal/LinkedListReversal_ForSketch4.sk",
//				"cvc3smtlib", true, false);
//	}

	@Test
	public void test4cvc3BlastBV() throws Exception {
		runToSmt(
				"Blast BV",
				"inputs/comparisons/LinkedListReversal/LinkedListReversal_ForSketch4.sk",
				"cvc3smtlib", false, true);
	}

	@Test
	public void test4cvc3BlastInt() throws Exception {
		runToSmt(
				"Blast Int",
				"inputs/comparisons/LinkedListReversal/LinkedListReversal_ForSketch4.sk",
				"cvc3smtlib", false, false);
	}

//	@Test
//	public void test5cvc3TOABV() throws Exception {
//		runTest(
//				"TOA BV",
//				"inputs/comparisons/LinkedListReversal/LinkedListReversal_ForSketch5.sk",
//				"cvc3smtlib", true, true);
//	}
//
//	@Test
//	public void test5cvc3TOAInt() throws Exception {
//		runTest(
//				"TOA Int",
//				"inputs/comparisons/LinkedListReversal/LinkedListReversal_ForSketch5.sk",
//				"cvc3smtlib", true, false);
//	}

	@Test
	public void test5cvc3BlastBV() throws Exception {
		runToSmt(
				"Blast BV",
				"inputs/comparisons/LinkedListReversal/LinkedListReversal_ForSketch5.sk",
				"cvc3smtlib", false, true);
	}

	@Test
	public void test5cvc3BlastInt() throws Exception {
		runToSmt(
				"Blast Int",
				"inputs/comparisons/LinkedListReversal/LinkedListReversal_ForSketch5.sk",
				"cvc3smtlib", false, false);
	}

//	@Test
//	public void test6cvc3TOABV() throws Exception {
//		runTest(
//				"TOA BV",
//				"inputs/comparisons/LinkedListReversal/LinkedListReversal_ForSketch6.sk",
//				"cvc3smtlib", true, true);
//	}
//
//	@Test
//	public void test6cvc3TOAInt() throws Exception {
//		runTest(
//				"TOA Int",
//				"inputs/comparisons/LinkedListReversal/LinkedListReversal_ForSketch6.sk",
//				"cvc3smtlib", true, false);
//	}
//
//	@Test
//	public void test6cvc3BlastBV() throws Exception {
//		runTest(
//				"Blast BV",
//				"inputs/comparisons/LinkedListReversal/LinkedListReversal_ForSketch6.sk",
//				"cvc3smtlib", false, true);
//	}
//
//	@Test
//	public void test6cvc3BlastInt() throws Exception {
//		runTest(
//				"Blast Int",
//				"inputs/comparisons/LinkedListReversal/LinkedListReversal_ForSketch6.sk",
//				"cvc3smtlib", false, false);
//	}
	
	// SBit
	@Test
	public void test1SBit() throws Exception {
		runSBit("inputs/comparisons/LinkedListReversal/LinkedListReversal_ForSketch1.sk");
	}
	
	@Test
	public void test2SBit() throws Exception {
		runSBit("inputs/comparisons/LinkedListReversal/LinkedListReversal_ForSketch2.sk");
	}

	@Test
	public void test3SBit() throws Exception {
		runSBit("inputs/comparisons/LinkedListReversal/LinkedListReversal_ForSketch3.sk");
	}
	
	@Test
	public void test4SBit() throws Exception {
		runSBit("inputs/comparisons/LinkedListReversal/LinkedListReversal_ForSketch4.sk");
	}
	
	@Test
	public void test5SBit() throws Exception {
		runSBit("inputs/comparisons/LinkedListReversal/LinkedListReversal_ForSketch5.sk");
	}
//	@Test
//	public void test6SBit() throws Exception {
//		runSBit("inputs/comparisons/LinkedListReversal/LinkedListReversal_ForSketch6.sk");
//	}

	// / Yices
	@Test
	public void test1yicesTOABV() throws Exception {
		runToSmt(
				"TOA BV",
				"inputs/comparisons/LinkedListReversal/LinkedListReversal_ForSketch1.sk",
				"yices", true, true);
	}

	@Test
	public void test1yicesTOAInt() throws Exception {
		runToSmt(
				"TOA Int",
				"inputs/comparisons/LinkedListReversal/LinkedListReversal_ForSketch1.sk",
				"yices", true, false);
	}

	@Test
	public void test1yicesBlastBV() throws Exception {
		runToSmt(
				"Blast BV",
				"inputs/comparisons/LinkedListReversal/LinkedListReversal_ForSketch1.sk",
				"yices", false, true);
	}

	@Test
	public void test1yicesBlastInt() throws Exception {
		runToSmt(
				"Blast Int",
				"inputs/comparisons/LinkedListReversal/LinkedListReversal_ForSketch1.sk",
				"yices", false, false);
	}

	@Test
	public void test2yicesTOABV() throws Exception {
		runToSmt(
				"TOA BV",
				"inputs/comparisons/LinkedListReversal/LinkedListReversal_ForSketch2.sk",
				"yices", true, true);
	}

	@Test
	public void test2yicesTOAInt() throws Exception {
		runToSmt(
				"TOA Int",
				"inputs/comparisons/LinkedListReversal/LinkedListReversal_ForSketch2.sk",
				"yices", true, false);
	}

	@Test
	public void test2yicesBlastBV() throws Exception {
		runToSmt(
				"Blast BV",
				"inputs/comparisons/LinkedListReversal/LinkedListReversal_ForSketch2.sk",
				"yices", false, true);
	}

	@Test
	public void test2yicesBlastInt() throws Exception {
		runToSmt(
				"Blast Int",
				"inputs/comparisons/LinkedListReversal/LinkedListReversal_ForSketch2.sk",
				"yices", false, false);
	}

	@Test
	public void test3yicesTOABV() throws Exception {
		runToSmt(
				"TOA BV",
				"inputs/comparisons/LinkedListReversal/LinkedListReversal_ForSketch3.sk",
				"yices", true, true);
	}

	@Test
	public void test3yicesTOAInt() throws Exception {
		runToSmt(
				"TOA Int",
				"inputs/comparisons/LinkedListReversal/LinkedListReversal_ForSketch3.sk",
				"yices", true, false);
	}

	@Test
	public void test3yicesBlastBV() throws Exception {
		runToSmt(
				"Blast BV",
				"inputs/comparisons/LinkedListReversal/LinkedListReversal_ForSketch3.sk",
				"yices", false, true);
	}

	@Test
	public void test3yicesBlastInt() throws Exception {
		runToSmt(
				"Blast Int",
				"inputs/comparisons/LinkedListReversal/LinkedListReversal_ForSketch3.sk",
				"yices", false, false);
	}

	@Test
	public void test4yicesTOABV() throws Exception {
		runToSmt(
				"TOA BV",
				"inputs/comparisons/LinkedListReversal/LinkedListReversal_ForSketch4.sk",
				"yices", true, true);
	}

	@Test
	public void test4yicesTOAInt() throws Exception {
		runToSmt(
				"TOA Int",
				"inputs/comparisons/LinkedListReversal/LinkedListReversal_ForSketch4.sk",
				"yices", true, false);
	}

	@Test
	public void test4yicesBlastBV() throws Exception {
		runToSmt(
				"Blast BV",
				"inputs/comparisons/LinkedListReversal/LinkedListReversal_ForSketch4.sk",
				"yices", false, true);
	}

	@Test
	public void test4yicesBlastInt() throws Exception {
		runToSmt(
				"Blast Int",
				"inputs/comparisons/LinkedListReversal/LinkedListReversal_ForSketch4.sk",
				"yices", false, false);
	}

	@Test
	public void test5yicesTOABV() throws Exception {
		runToSmt(
				"TOA BV",
				"inputs/comparisons/LinkedListReversal/LinkedListReversal_ForSketch5.sk",
				"yices", true, true);
	}

	@Test
	public void test5yicesTOAInt() throws Exception {
		runToSmt(
				"TOA Int",
				"inputs/comparisons/LinkedListReversal/LinkedListReversal_ForSketch5.sk",
				"yices", true, false);
	}

	@Test
	public void test5yicesBlastBV() throws Exception {
		runToSmt(
				"Blast BV",
				"inputs/comparisons/LinkedListReversal/LinkedListReversal_ForSketch5.sk",
				"yices", false, true);
	}

	@Test
	public void test5yicesBlastInt() throws Exception {
		runToSmt(
				"Blast Int",
				"inputs/comparisons/LinkedListReversal/LinkedListReversal_ForSketch5.sk",
				"yices", false, false);
	}

	// / z3
//	@Test
	public void test1z3TOABV() throws Exception {
		runToSmt(
				"TOA BV",
				"inputs/comparisons/LinkedListReversal/LinkedListReversal_ForSketch1.sk",
				"z3", true, true);
	}

//	@Test
	public void test1z3TOAInt() throws Exception {
		runToSmt(
				"TOA Int",
				"inputs/comparisons/LinkedListReversal/LinkedListReversal_ForSketch1.sk",
				"z3", true, false);
	}

	@Test
	public void test1z3BlastBV() throws Exception {
		runToSmt(
				"Blast BV",
				"inputs/comparisons/LinkedListReversal/LinkedListReversal_ForSketch1.sk",
				"z3", false, true);
	}

	@Test
	public void test1z3BlastInt() throws Exception {
		runToSmt(
				"Blast Int",
				"inputs/comparisons/LinkedListReversal/LinkedListReversal_ForSketch1.sk",
				"z3", false, false);
	}

//	@Test
	public void test2z3TOABV() throws Exception {
		runToSmt(
				"TOA BV",
				"inputs/comparisons/LinkedListReversal/LinkedListReversal_ForSketch2.sk",
				"z3", true, true);
	}

//	@Test
	public void test2z3TOAInt() throws Exception {
		runToSmt(
				"TOA Int",
				"inputs/comparisons/LinkedListReversal/LinkedListReversal_ForSketch2.sk",
				"z3", true, false);
	}

	@Test
	public void test2z3BlastBV() throws Exception {
		runToSmt(
				"Blast BV",
				"inputs/comparisons/LinkedListReversal/LinkedListReversal_ForSketch2.sk",
				"z3", false, true);
	}

	@Test
	public void test2z3BlastInt() throws Exception {
		runToSmt(
				"Blast Int",
				"inputs/comparisons/LinkedListReversal/LinkedListReversal_ForSketch2.sk",
				"z3", false, false);
	}

	@Test
	public void test3z3TOABV() throws Exception {
		runToSmt(
				"TOA BV",
				"inputs/comparisons/LinkedListReversal/LinkedListReversal_ForSketch3.sk",
				"z3", true, true);
	}

	@Test
	public void test3z3TOAInt() throws Exception {
		runToSmt(
				"TOA Int",
				"inputs/comparisons/LinkedListReversal/LinkedListReversal_ForSketch3.sk",
				"z3", true, false);
	}

	@Test
	public void test3z3BlastBV() throws Exception {
		runToSmt(
				"Blast BV",
				"inputs/comparisons/LinkedListReversal/LinkedListReversal_ForSketch3.sk",
				"z3", false, true);
	}

	@Test
	public void test3z3BlastInt() throws Exception {
		runToSmt(
				"Blast Int",
				"inputs/comparisons/LinkedListReversal/LinkedListReversal_ForSketch3.sk",
				"z3", false, false);
	}

//	@Test
	public void test4z3TOABV() throws Exception {
		runToSmt(
				"TOA BV",
				"inputs/comparisons/LinkedListReversal/LinkedListReversal_ForSketch4.sk",
				"z3", true, true);
	}

//	@Test
	public void test4z3TOAInt() throws Exception {
		runToSmt(
				"TOA Int",
				"inputs/comparisons/LinkedListReversal/LinkedListReversal_ForSketch4.sk",
				"z3", true, false);
	}

	@Test
	public void test4z3BlastBV() throws Exception {
		runToSmt(
				"Blast BV",
				"inputs/comparisons/LinkedListReversal/LinkedListReversal_ForSketch4.sk",
				"z3", false, true);
	}

	@Test
	public void test4z3BlastInt() throws Exception {
		runToSmt(
				"Blast Int",
				"inputs/comparisons/LinkedListReversal/LinkedListReversal_ForSketch4.sk",
				"z3", false, false);
	}

//	@Test
	public void test5z3TOABV() throws Exception {
		runToSmt(
				"TOA BV",
				"inputs/comparisons/LinkedListReversal/LinkedListReversal_ForSketch5.sk",
				"z3", true, true);
	}

//	@Test
	public void test5z3TOAInt() throws Exception {
		runToSmt(
				"TOA Int",
				"inputs/comparisons/LinkedListReversal/LinkedListReversal_ForSketch5.sk",
				"z3", true, false);
	}

	@Test
	public void test5z3BlastBV() throws Exception {
		runToSmt(
				"Blast BV",
				"inputs/comparisons/LinkedListReversal/LinkedListReversal_ForSketch5.sk",
				"z3", false, true);
	}

	@Test
	public void test5z3BlastInt() throws Exception {
		runToSmt(
				"Blast Int",
				"inputs/comparisons/LinkedListReversal/LinkedListReversal_ForSketch5.sk",
				"z3", false, false);
	}
//	@Test
//	public void test6yicesTOABV() throws Exception {
//		runTest(
//				"TOA BV",
//				"inputs/comparisons/LinkedListReversal/LinkedListReversal_ForSketch6.sk",
//				"yices", true, true);
//	}
//	@Test
//	public void test6yicesTOAInt() throws Exception {
//		runTest(
//				"TOA Int",
//				"inputs/comparisons/LinkedListReversal/LinkedListReversal_ForSketch6.sk",
//				"yices", true, false);
//	}
//
//	@Test
//	public void test6yicesBlastBV() throws Exception {
//		runTest(
//				"Blast BV",
//				"inputs/comparisons/LinkedListReversal/LinkedListReversal_ForSketch6.sk",
//				"yices", false, true);
//	}
//
//	@Test
//	public void test6yicesBlastInt() throws Exception {
//		runTest(
//				"Blast Int",
//				"inputs/comparisons/LinkedListReversal/LinkedListReversal_ForSketch6.sk",
//				"yices", false, false);
//	}

}
