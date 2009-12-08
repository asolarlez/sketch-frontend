package sketch.compiler.smt.tests;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashMap;

import org.junit.After;
import org.junit.Test;

import sketch.compiler.main.seq.SequentialSMTSketchMain;
import sketch.compiler.smt.CEGISLoop;
import sketch.compiler.smt.HoleSorter;
import sketch.compiler.smt.TestHarness;
import sketch.compiler.smt.partialeval.SmtValueOracle;

public class RegularExprBlastBV extends TestHarness {

	protected HashMap<String, String> initCmdArgs(String input) {
		String inputPath = "inputs/regexprs/" + input;
	
		HashMap<String, String> argsMap = new HashMap<String, String>();

		argsMap.put("--smtpath", System.getenv("smtpath"));
		
		argsMap.put("--bv", null);
		argsMap.put("--arrayOOBPolicy", "assertions");

		argsMap.put("--heapsize", "10");
		argsMap.put("--intbits", "5");

		argsMap.put("--outputdir", "output/");
		argsMap.put("--tmpdir", tmpDirStr);
		argsMap.put("--keeptmpfiles", null);

		
		// argsMap.put("--trace", null);
		argsMap.put(inputPath, null);

		return argsMap;
	}
	
	public void runOneTest(String[] args) throws IOException, InterruptedException {
		oracle = null;
		stat = null;
		SequentialSMTSketchMain main = new SequentialSMTSketchMain(args);
		assertTrue(main.runBeforeGenerateCode());
		oracle = (SmtValueOracle) main.getOracle();
		stat = main.getSolutionStat();
	}
	
	@After
	public void printTiming() {
		if (stat == null)
			System.out.println("\tFAILED");
		else
			System.out.println("\t" + 
			        (stat.getLong(CEGISLoop.VERIFICATION_TIME) + stat.getLong(CEGISLoop.SYNTHESIS_TIME)) + "\t" + 
			        stat.getLong(CEGISLoop.CEGIS_ITR));
	}
	
	
	@Test
	public void testSingleOpBinaryEq() throws Exception {
		HashMap<String, String> argsMap = initCmdArgs("singleOpBinaryEq.sk");

		runOneTest(toArgArray(argsMap));
		
		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue(sorter.getHoleValueByOrder(0) == 1);
		assertTrue(sorter.getHoleValueByOrder(1) == 1);
	}
	
	@Test
	public void testSingleOpBinaryLt() throws Exception {
		HashMap<String, String> argsMap = initCmdArgs("singleOpBinaryLt.sk");

		runOneTest(toArgArray(argsMap));
		
		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue(sorter.getHoleValueByOrder(0) == 0);
		assertTrue(sorter.getHoleValueByOrder(1) == 2);
	}
	
	@Test
	public void testSingleOpBinaryLe() throws Exception {
		HashMap<String, String> argsMap = initCmdArgs("singleOpBinaryLe.sk");

		runOneTest(toArgArray(argsMap));

		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue(sorter.getHoleValueByOrder(0) == 2);
		assertTrue(sorter.getHoleValueByOrder(1) == 1);
	}
	
	@Test
	public void testSingleOpBinaryGt() throws Exception {
		HashMap<String, String> argsMap = initCmdArgs("singleOpBinaryGt.sk");

		runOneTest(toArgArray(argsMap));
		
		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue(sorter.getHoleValueByOrder(0) == 2);
		assertTrue(sorter.getHoleValueByOrder(1) == 1);
	}
	
	@Test
	public void testSingleOpBinaryGe() throws Exception {
		HashMap<String, String> argsMap = initCmdArgs("singleOpBinaryGe.sk");

		runOneTest(toArgArray(argsMap));
		
		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue(sorter.getHoleValueByOrder(0) == 2);
		assertTrue(sorter.getHoleValueByOrder(1) == 2);		
	}
	
	@Test
	public void testSingleOpBinaryAnd() throws Exception {
		HashMap<String, String> argsMap = initCmdArgs("singleOpBinaryAnd.sk");

		runOneTest(toArgArray(argsMap));
		
		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue(sorter.getHoleValueByOrder(0) == 1);
		assertTrue(sorter.getHoleValueByOrder(1) == 1);
	}
	
	@Test
	public void testSingleOpBinaryOr() throws Exception {
		HashMap<String, String> argsMap = initCmdArgs("singleOpBinaryOr.sk");

		runOneTest(toArgArray(argsMap));
	
		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue(sorter.getHoleValueByOrder(0) == 2);
		assertTrue(sorter.getHoleValueByOrder(1) == 1);
	}
	
	@Test
	public void testDoubleOpBinaryEq() throws Exception {
		HashMap<String, String> argsMap = initCmdArgs("doubleOpBinaryEq.sk");

		runOneTest(toArgArray(argsMap));
		
		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue(sorter.getHoleValueByOrder(0) == 1);
		assertTrue(sorter.getHoleValueByOrder(1) == 1);
	}
	
	@Test
	public void testDoubleOpBinaryLt() throws Exception {
		HashMap<String, String> argsMap = initCmdArgs("doubleOpBinaryLt.sk");

		runOneTest(toArgArray(argsMap));
		
		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue(sorter.getHoleValueByOrder(0) == 1);
		assertTrue(sorter.getHoleValueByOrder(1) == 1);
	}
	
	@Test
	public void testDoubleOpBinaryLe() throws Exception {
		HashMap<String, String> argsMap = initCmdArgs("doubleOpBinaryLe.sk");

		runOneTest(toArgArray(argsMap));
		
		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue(sorter.getHoleValueByOrder(0) == 1);
		assertTrue(sorter.getHoleValueByOrder(1) == 1);
	}
	
	@Test
	public void testDoubleOpBinaryGt() throws Exception {
		HashMap<String, String> argsMap = initCmdArgs("doubleOpBinaryGt.sk");

		runOneTest(toArgArray(argsMap));
		
		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue(sorter.getHoleValueByOrder(0) == 0);
		assertTrue(sorter.getHoleValueByOrder(1) == 1);
	}
	
	@Test
	public void testDoubleOpBinaryGe() throws Exception {
		HashMap<String, String> argsMap = initCmdArgs("doubleOpBinaryGe.sk");

		runOneTest(toArgArray(argsMap));
		
		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue(sorter.getHoleValueByOrder(0) == 1);
		assertTrue(sorter.getHoleValueByOrder(1) == 0);
	}
	
	@Test
	public void testCEGISLoop() throws Exception {
		HashMap<String, String> argsMap = initCmdArgs("cegisLoop.sk");

		runOneTest(toArgArray(argsMap));
		
		HoleSorter sorter = new HoleSorter(oracle);		
		assertTrue(sorter.getHoleValueByOrder(0) == 1);
	}
}
