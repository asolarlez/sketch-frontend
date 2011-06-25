package sketch.compiler.smt.tests;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashMap;

import org.junit.Test;

import sketch.compiler.main.seq.SequentialSMTSketchMain;
import sketch.compiler.smt.TestHarness;

public class BitVectorBlastBV extends TestHarness {

	protected HashMap<String, String> initCmdArgs(String input) {
	    String inputPath = "src/test/sk/smt/bitvectors/" + input;
		HashMap<String, String> argsMap = new HashMap<String, String>();
		
		argsMap.put("--smtpath", System.getenv("smtpath"));
		argsMap.put("--bv", null);		
		argsMap.put("--arrayOOBPolicy", "assertions");
		
		argsMap.put("--heapsize", "10");
		argsMap.put("--inbits", "32");
		argsMap.put("--intbits", "10");
		argsMap.put("--cbits", "32");
		
		argsMap.put("--outputdir", "output/");
		argsMap.put("--tmpdir", tmpDirStr);
		argsMap.put("--keeptmpfiles", null);
		
		argsMap.put(inputPath, null);
		
		return argsMap;
	}
	
	private void runOneTest(HashMap<String, String> argsMap)
		throws IOException, InterruptedException {
		SequentialSMTSketchMain main = new SequentialSMTSketchMain(toArgArray(argsMap));
		assertTrue(main.runBeforeGenerateCode());
	}
	
	@Test
	public void testInit() throws Exception {
		HashMap<String, String> argsMap = initCmdArgs("init.sk");

		runOneTest(argsMap);
	}
	
	@Test
	public void testVectorIndex() throws Exception {
		HashMap<String, String> argsMap = initCmdArgs("vectorIndex.sk");

		runOneTest(argsMap);
	}
	
	@Test
	public void testArrayAssignment() throws Exception {
		HashMap<String, String> argsMap = initCmdArgs("arrayAssignment.sk");

		runOneTest(argsMap);
	}
	
	@Test
	public void testCastConstScalarToBitArray() throws Exception {
		HashMap<String, String> argsMap = initCmdArgs("castConstScalarToBitArray.sk");

		runOneTest(argsMap);
	}
	
	@Test
	public void testCastBottomScalarToBitArray() throws Exception {
		HashMap<String, String> argsMap = initCmdArgs("castBottomScalarToBitArray.sk");

		runOneTest(argsMap);
	}
	
	@Test
	public void testCastBigBitArrayToSmall() throws Exception {
		HashMap<String, String> argsMap = initCmdArgs("castBigBitArrayToSmall.sk");

		runOneTest(argsMap);
	}
	
	@Test
	public void testCastSmallBitArrayToBig() throws Exception {
		HashMap<String, String> argsMap = initCmdArgs("castSmallBitArrayToBig.sk");

		runOneTest(argsMap);
	}
	
	@Test
	public void testElementUpdateWithConstIdx() throws Exception {
		HashMap<String, String> argsMap = initCmdArgs("elementUpdateWithConstIdx.sk");

		runOneTest(argsMap);
	}
	
	@Test
	public void testElementAccwWithBottomIdx() throws Exception {
		HashMap<String, String> argsMap = initCmdArgs("elementAccWithBottomIdx.sk");

		runOneTest(argsMap);
	}
	
	@Test
	public void testElementRangeAccWithConstRange() throws Exception {
		HashMap<String, String> argsMap = initCmdArgs("elementRangeAccWithConstRange.sk");

		runOneTest(argsMap);
	}
	
	@Test
	public void testElementRangeUpdateWithConstRange() throws Exception {
		HashMap<String, String> argsMap = initCmdArgs("elementRangeUpdateWithConstRange.sk");

		runOneTest(argsMap);
	}
	
	@Test
	public void testElementUpdateWithBottomIdx() throws Exception {
		HashMap<String, String> argsMap = initCmdArgs("elementUpdateWithBottomIdx.sk");

		runOneTest(argsMap);
	}
	
	@Test
	public void testHole() throws Exception {
		HashMap<String, String> argsMap = initCmdArgs("hole.sk");

		runOneTest(argsMap);
	}

}
