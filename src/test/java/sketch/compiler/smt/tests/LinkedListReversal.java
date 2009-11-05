package sketch.compiler.smt.tests;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashMap;

import org.junit.After;
import org.junit.Test;

import sketch.compiler.main.seq.SequentialSMTSketchMain;
import sketch.compiler.smt.TestHarness;
import sketch.compiler.smt.partialeval.SmtValueOracle;

public class LinkedListReversal extends TestHarness {
	protected HashMap<String, String> initCmdArgs(String input) {
		String inputPath = "inputs/comparisons/LinkedListReversal/" + input;
		HashMap<String, String> argsMap = new HashMap<String, String>();

		argsMap.put("--smtpath", System.getenv("smtpath"));
		argsMap.put("--backend", "yices");
		argsMap.put("--bv", null);
		argsMap.put("--arrayOOBPolicy", "assertions");

		argsMap.put("--heapsize", "10");
		argsMap.put("--inbits", "32");
		argsMap.put("--intbits", "5");

		argsMap.put("--outputdir", "output/");
		argsMap.put("--tmpdir", System.getenv("tmpdir"));
		argsMap.put("--keeptmpfiles", null);

		// argsMap.put("--verbosity", "4");
		// argsMap.put("--showphase", "lowering");
		// argsMap.put("--trace", null);
		argsMap.put(inputPath, null);

		return argsMap;
	}

	static String backend = "yices";

	private void runToSmt(String input) throws IOException,
			InterruptedException {
		 
		HashMap<String, String> argsMap = initCmdArgs(input);
		SequentialSMTSketchMain main = new SequentialSMTSketchMain(toArgArray(argsMap));
		assertTrue(main.runBeforeGenerateCode());
		stat = main.getSolutionStat();
		oracle = (SmtValueOracle) main.getOracle();
		
	}
	
	@After
	public void printTiming() {
		System.out.println("\t" + stat.getSolutionTimeMs() + "\t" + stat.getIterations());	
	}

	@Test
	public void llr() throws Exception {
		String input = "LinkedListReversal_ForSketch.sk";
		runToSmt(input);
	}
	
	@Test
	public void llr1() throws Exception {
		String input = "LinkedListReversal_ForSketch1.sk";
		runToSmt(input);
	}
	
	@Test
	public void llr2() throws Exception {
		String input = "LinkedListReversal_ForSketch2.sk";
		runToSmt(input);
	}
	
	@Test
	public void llr3() throws Exception {
		String input = "LinkedListReversal_ForSketch3.sk";
		runToSmt(input);
	}
	
	@Test
	public void llr4() throws Exception {
		String input = "LinkedListReversal_ForSketch4.sk";
		runToSmt(input);
	}
	
	@Test
	public void llr5() throws Exception {
		String input = "LinkedListReversal_ForSketch5.sk";
		runToSmt(input);
	}
}
