package sketch.compiler.smt.yices;

import java.io.IOException;
import java.util.HashMap;

import junit.framework.Assert;

public class LanguageBasicBlastBV extends sketch.compiler.smt.tests.LanguageBasicBlastBV  {

	protected HashMap<String, String> initCmdArgs(String input) {
		HashMap<String, String> argsMap = super.initCmdArgs(input);
		argsMap.put("--backend", "yices");
		
		 argsMap.put("--verbosity", "4");
		 argsMap.put("--showphase", "lowering");
		System.out.print(input + "\tyices");
		return argsMap;
	}
	
	@Override
	public void testIntegers() throws Exception {
	    Assert.fail("not showing full model");
		super.testIntegers();
	}
	
	@Override
	public void testRegExpr() throws Exception {
        Assert.fail("not showing full model");
	    super.testRegExpr();
	}
	
	@Override
	public void testRegExpr3() throws Exception {
	    Assert.fail("not showing full model");
	    super.testRegExpr3();
	}
	
	@Override
	public void testRegExprObj2() throws Exception {
	    Assert.fail("not showing full model");
	    super.testRegExprObj2();
	}
	
	@Override
	public void testConvertBitArrayToInt3() throws IOException,
	        InterruptedException
	{
	    Assert.fail("not showing full model");
	    super.testConvertBitArrayToInt3();
	}
	
	@Override
	public void testFunctionCall() throws Exception {
	    Assert.fail("not showing full model");
	    super.testFunctionCall();
	}
	
	@Override
	public void testFuncCallDupRecursionEz() throws Exception {
	    Assert.fail("not showing full model");
	    super.testFuncCallDupRecursionEz();
	}
	
	@Override
	public void testObjectThreeFields() throws Exception {
	    Assert.fail("not showing full model");
	    super.testObjectThreeFields();
	}
	
	@Override
	public void testObjectWhile() throws Exception {
	    Assert.fail("not showing full model");
	    super.testObjectWhile();
	}
	
	@Override
	public void testHoleWhile() throws Exception {
	    Assert.fail("not showing full model");
	    super.testHoleWhile();
	}
	
	@Override
	public void testArrayAsInput() throws Exception {
	    Assert.fail("not showing full model");
	    super.testArrayAsInput();
	}
	
	@Override
	public void testArrayBoundCheck() throws Exception {
	    Assert.fail("not showing full model");
	    super.testArrayBoundCheck();
	}
	
	@Override
	public void testArray() throws Exception {
	    Assert.fail("not showing full model");
	    super.testArray();
	}
	
	@Override
	public void testArrayAssignToArray() throws Exception {
	    Assert.fail("not showing full model");
	    super.testArrayAssignToArray();
	}
	
	@Override
	public void testIfStmtCEGIS() throws Exception {
	    Assert.fail("not showing full model");
	    super.testIfStmtCEGIS();
	}
	
	@Override
	public void testMultiAsserts() throws Exception {
	    Assert.fail("not showing full model");
	    super.testMultiAsserts();
	}
	
	
}
