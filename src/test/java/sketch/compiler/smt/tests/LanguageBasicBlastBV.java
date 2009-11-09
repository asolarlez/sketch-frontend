package sketch.compiler.smt.tests;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashMap;

import org.junit.Test;

import sketch.compiler.main.seq.SequentialSMTSketchMain;
import sketch.compiler.smt.HoleSorter;
import sketch.compiler.smt.TestHarness;
import sketch.compiler.smt.partialeval.SmtValueOracle;

public abstract class LanguageBasicBlastBV extends TestHarness {

	protected HashMap<String, String> initCmdArgs(String input) {
		String inputPath = "src/test/sk/smt/basics/" + input;

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

	public void runOneTest(String[] args) throws IOException,
			InterruptedException {
		oracle = null;
		stat = null;
		SequentialSMTSketchMain main = new SequentialSMTSketchMain(args);
		assertTrue(main.runBeforeGenerateCode());
		oracle = (SmtValueOracle) main.getOracle();
		stat = main.getSolutionStat();
		main.eliminateStar();
	}

	@Test
	public void testConcat() throws Exception {
		HashMap<String, String> argsMap = initCmdArgs("concat.sk");
		runOneTest(toArgArray(argsMap));

		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue(sorter.getHoleValueByOrder(0) == 30);
	}

	@Test
	public void testConvertBitArrayToInt() throws IOException,
			InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("convertBitArrayToInt.sk");
		runOneTest(toArgArray(argsMap));

	}

	@Test
	public void testConvertBitArrayToInt2() throws IOException,
			InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("convertBitArrayToInt2.sk");
		runOneTest(toArgArray(argsMap));

	}

	@Test
	public void testConvertBitArrayToInt3() throws IOException,
			InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("convertBitArrayToInt3.sk");

		String[] args = toArgArray(argsMap);
		SequentialSMTSketchMain main = new SequentialSMTSketchMain(args);
		assertTrue(main.runBeforeGenerateCode());

		SmtValueOracle oracle = (SmtValueOracle) main.getOracle();
		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue(sorter.getHoleValueByOrder(0) != 0);
		assertTrue(sorter.getHoleValueByOrder(1) == 0);
		assertTrue(sorter.getHoleValueByOrder(2) != 0);
	}

//	@Test
//	public void testConvertIntToBitArray() throws IOException,
//			InterruptedException {
//		HashMap<String, String> argsMap = initCmdArgs("convertIntToBitArray.sk");
//
//		String[] args = toArgArray(argsMap);
//		SequentialSMTSketchMain main = new SequentialSMTSketchMain(args);
//		assertTrue(main.runBeforeGenerateCode());
//	}
	
	@Test
	public void testFunctionCall() throws Exception {
		HashMap<String, String> argsMap = initCmdArgs("functionCall.sk");
		runOneTest(toArgArray(argsMap));

		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue(sorter.getHoleValueByOrder(0) == 40);
	}
	
	@Test
	public void testFuncCallDupRecursionEz() throws Exception{
		HashMap<String, String> argsMap = initCmdArgs("funcCallDupRecursionEz.sk");
		runOneTest(toArgArray(argsMap));
	}

	@Test
	public void testEmpty() throws Exception {
		HashMap<String, String> argsMap = initCmdArgs("empty.sk");
		runOneTest(toArgArray(argsMap));
	}

	@Test
	public void testExtract() throws Exception {
		HashMap<String, String> argsMap = initCmdArgs("extract.sk");
		runOneTest(toArgArray(argsMap));

		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue(sorter.getHoleValueByOrder(0) != 0);
	}

	@Test
	public void testIfStmt() throws Exception {
		HashMap<String, String> argsMap = initCmdArgs("ifStmt.sk");
		runOneTest(toArgArray(argsMap));

		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue(sorter.getHoleValueByOrder(0) == 30);

	}

	@Test
	public void testIfStmtNested() throws Exception {
		HashMap<String, String> argsMap = initCmdArgs("ifStmtNested.sk");
		runOneTest(toArgArray(argsMap));

		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue(sorter.getHoleValueByOrder(0) == 3);
		assertTrue(sorter.getHoleValueByOrder(1) == 9);
	}

	@Test
	public void testIfStmtWithArrayElemUpdate() throws Exception {
		HashMap<String, String> argsMap = initCmdArgs("ifStmtWithArrayElemUpdate.sk");
		runOneTest(toArgArray(argsMap));

		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue(sorter.getHoleValueByOrder(0) == 30);

	}

	@Test
	public void testIntegers() throws Exception {
		HashMap<String, String> argsMap = initCmdArgs("integers.sk");
		runOneTest(toArgArray(argsMap));

		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue(sorter.getHoleValueByOrder(0) == 30);
		assertTrue(sorter.getHoleValueByOrder(1) == 40);
	}

	@Test
	public void testIntegersIf() throws Exception {
		HashMap<String, String> argsMap = initCmdArgs("integersIf.sk");
		runOneTest(toArgArray(argsMap));

		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue(sorter.getHoleValueByOrder(1) == 30);
		assertTrue(sorter.getHoleValueByOrder(0) == 30);

	}

	@Test
	public void testObjectOneField() throws Exception {
		HashMap<String, String> argsMap = initCmdArgs("objectOneField.sk");
		runOneTest(toArgArray(argsMap));

		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue(sorter.getHoleValueByOrder(0) == 10);
	}

	@Test
	public void testObjectTwoFields() throws Exception {
		HashMap<String, String> argsMap = initCmdArgs("objectTwoFields.sk");
		runOneTest(toArgArray(argsMap));

		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue(sorter.getHoleValueByOrder(0) == 1666667);
	}

	@Test
	public void testObjectThreeFields() throws Exception {
		HashMap<String, String> argsMap = initCmdArgs("objectThreeFields.sk");
		runOneTest(toArgArray(argsMap));

		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue(sorter.getHoleValueByOrder(0) == 1666667);
		assertTrue(sorter.getHoleValueByOrder(1) == 1984);
	}

	@Test
	public void testBinaryOps() throws Exception {
		HashMap<String, String> argsMap = initCmdArgs("binaryOps.sk");
		runOneTest(toArgArray(argsMap));

		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue(sorter.getHoleValueByOrder(0) == 20);
		assertTrue(sorter.getHoleValueByOrder(1) == 10);
		assertTrue(sorter.getHoleValueByOrder(2) == 4);

	}

	@Test
	public void testBinaryDivide() throws Exception {
		HashMap<String, String> argsMap = initCmdArgs("binaryDivide.sk");
		runOneTest(toArgArray(argsMap));

		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue(sorter.getHoleValueByOrder(0) == 6
				|| sorter.getHoleValueByOrder(0) == 7);
	}

	@Test
	public void testBinaryXOR() throws Exception {
		HashMap<String, String> argsMap = initCmdArgs("binaryXOR.sk");
		runOneTest(toArgArray(argsMap));

		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue(sorter.getHoleValueByOrder(0) == 1);
	}

	@Test
	public void testRegExpr() throws Exception {
		HashMap<String, String> argsMap = initCmdArgs("regexpr.sk");
		runOneTest(toArgArray(argsMap));

		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue(sorter.getHoleValueByOrder(0) == 1);
	}

	@Test
	public void testRegExpr3() throws Exception {
		HashMap<String, String> argsMap = initCmdArgs("regexpr3.sk");
		runOneTest(toArgArray(argsMap));

		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue(sorter.getHoleValueByOrder(0) == 1);
	}

	@Test
	public void testRegExprObj2() throws Exception {
		HashMap<String, String> argsMap = initCmdArgs("regExprObj2.sk");
		runOneTest(toArgArray(argsMap));

		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue(sorter.getHoleValueByOrder(0) == 1);
	}

	@Test
	public void testNegativeInt() throws Exception {
		HashMap<String, String> argsMap = initCmdArgs("negativeInt.sk");
		runOneTest(toArgArray(argsMap));

		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue(sorter.getHoleValueByOrder(0) == 1000);
	}

	@Test
	public void testIntegerWhile() throws Exception {
		HashMap<String, String> argsMap = initCmdArgs("integerWhile.sk");
		runOneTest(toArgArray(argsMap));

		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue(sorter.getHoleValueByOrder(0) == 5);
	}

	@Test
	public void testObjectWhile() throws Exception {
		HashMap<String, String> argsMap = initCmdArgs("objectWhile.sk");
		runOneTest(toArgArray(argsMap));

		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue(sorter.getHoleValueByOrder(0) == 3);
	}

	@Test
	public void testHoleWhile() throws Exception {
		HashMap<String, String> argsMap = initCmdArgs("holeWhile.sk");
		runOneTest(toArgArray(argsMap));

		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue(sorter.getHoleValueByOrder(0) == 2);
	}

	@Test
	public void testArrayAsInput() throws Exception {
		HashMap<String, String> argsMap = initCmdArgs("arrayAsInput.sk");
		runOneTest(toArgArray(argsMap));

		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue(sorter.getHoleValueByOrder(0) == 100);
		assertTrue(sorter.getHoleValueByOrder(1) == 200);
	}

	@Test
	public void testArrayBoundCheck() throws Exception {
		HashMap<String, String> argsMap = initCmdArgs("arrayBoundCheck.sk");
		runOneTest(toArgArray(argsMap));

		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue(sorter.getHoleValueByOrder(0) == 1);
	}

	@Test
	public void testArray() throws Exception {
		HashMap<String, String> argsMap = initCmdArgs("array.sk");
		runOneTest(toArgArray(argsMap));

		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue(sorter.getHoleValueByOrder(0) == 99);
		assertTrue(sorter.getHoleValueByOrder(1) == 999);
	}

	@Test
	public void testArrayAssignToArray() throws Exception {
		HashMap<String, String> argsMap = initCmdArgs("arrayAssignToArray.sk");
		runOneTest(toArgArray(argsMap));

		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue(sorter.getHoleValueByOrder(0) == 3);
		assertTrue(sorter.getHoleValueByOrder(1) == 4);
	}

	@Test
	public void testArrayInit() throws Exception {
		HashMap<String, String> argsMap = initCmdArgs("arrayInit.sk");
		runOneTest(toArgArray(argsMap));

		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue(sorter.getHoleValueByOrder(0) == 1);
	}

	@Test
	public void testArrayInitWithStar() throws Exception {
		HashMap<String, String> argsMap = initCmdArgs("arrayInitWithStar.sk");
		runOneTest(toArgArray(argsMap));

		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue(sorter.getHoleValueByOrder(0) == 5);
		assertTrue(sorter.getHoleValueByOrder(1) == 4);
		assertTrue(sorter.getHoleValueByOrder(2) == 3);
		assertTrue(sorter.getHoleValueByOrder(3) == 2);
		assertTrue(sorter.getHoleValueByOrder(4) == 1);
	}

	@Test
	public void testArrayMultiDim() throws Exception {
		HashMap<String, String> argsMap = initCmdArgs("arrayMultiDim.sk");
		runOneTest(toArgArray(argsMap));

		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue(sorter.getHoleValueByOrder(0) == 1999);
		assertTrue(sorter.getHoleValueByOrder(1) == 2008);
	}

	@Test
	public void testArrayUpdateBottomIdx() throws Exception {
		HashMap<String, String> argsMap = initCmdArgs("arrayUpdateBottomIdx.sk");
		runOneTest(toArgArray(argsMap));

		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue(sorter.getHoleValueByOrder(0) == 5);

	}

	@Test
	public void testArrayAccessBottomIdx() throws Exception {
		HashMap<String, String> argsMap = initCmdArgs("arrayAccessBottomIdx.sk");
		runOneTest(toArgArray(argsMap));

		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue(sorter.getHoleValueByOrder(0) == 90);
	}
	
	@Test
	public void testBvIf() throws Exception {
		HashMap<String, String> argsMap = initCmdArgs("bvIf.sk");
		runOneTest(toArgArray(argsMap));

		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue(sorter.getHoleValueByOrder(0) != 0);
	}
	

	@Test
	public void testIfStmtCEGIS() throws Exception {
		HashMap<String, String> argsMap = initCmdArgs("ifStmtCEGIS.sk");
		runOneTest(toArgArray(argsMap));

		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue(sorter.getHoleValueByOrder(0) == 100);
	}

	@Test
	public void testMultiAsserts() throws Exception {
		HashMap<String, String> argsMap = initCmdArgs("multiAsserts.sk");
		runOneTest(toArgArray(argsMap));

		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue(sorter.getHoleValueByOrder(0) == 100);

	}

	@Test
	public void testTypeCastFromIntBottom() throws Exception {
		HashMap<String, String> argsMap = initCmdArgs("typecastFromIntBottom.sk");
		runOneTest(toArgArray(argsMap));

		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue(sorter.getHoleValueByOrder(0) == 7);
	}

	@Test
	public void testTypeCastFromBitBottom() throws Exception {
		HashMap<String, String> argsMap = initCmdArgs("typecastFromBitBottom.sk");
		runOneTest(toArgArray(argsMap));

		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue(sorter.getHoleValueByOrder(0) == 7);
	}

	@Test
	public void testTypeCastFromIntBoolBottom() throws Exception {
		HashMap<String, String> argsMap = initCmdArgs("typecastFromBoolBottom.sk");
		runOneTest(toArgArray(argsMap));

		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue(sorter.getHoleValueByOrder(0) == 7);
	}

	@Test
	public void testTypeCast() throws Exception {
		HashMap<String, String> argsMap = initCmdArgs("typecast.sk");
		runOneTest(toArgArray(argsMap));

		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue(sorter.getHoleValueByOrder(0) == 6);
	}

	@Test
	public void testUnaryOpsBitNot() throws Exception {
		HashMap<String, String> argsMap = initCmdArgs("unaryOpsBitNot.sk");
		runOneTest(toArgArray(argsMap));

		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue(sorter.getHoleValueByOrder(0) == 1);

	}

	@Test
	public void testUnaryOpsLogicalNot() throws Exception {
		HashMap<String, String> argsMap = initCmdArgs("unaryOpsLogicalNot.sk");
		runOneTest(toArgArray(argsMap));

		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue(sorter.getHoleValueByOrder(0) == 1);
	}

	@Test
	public void testUnaryOpsNegation() throws Exception {
		HashMap<String, String> argsMap = initCmdArgs("unaryOpsNegation.sk");
		runOneTest(toArgArray(argsMap));

		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue(sorter.getHoleValueByOrder(0) == 33);
	}

	@Test
	public void testVarAssignment() throws Exception {
		HashMap<String, String> argsMap = initCmdArgs("varAssignment.sk");
		runOneTest(toArgArray(argsMap));
	}
}
