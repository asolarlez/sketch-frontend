package sketch.compiler.smt.tests;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashMap;

import org.junit.After;
import org.junit.Test;

import sketch.compiler.main.sten.StencilSmtSketchMain;
import sketch.compiler.smt.HoleSorter;
import sketch.compiler.smt.TestHarness;
import sketch.compiler.smt.partialeval.SmtValueOracle;

public class StenRegressionTests extends TestHarness {
	protected HashMap<String, String> initCmdArgs(String input) {
		String inputPath = "src/test/sk/sten/" + input;
		
		HashMap<String, String> argsMap = new HashMap<String, String>();
		
		argsMap.put("--smtpath", System.getenv("smtpath"));
		argsMap.put("--backend", "yices");
		argsMap.put("--bv", null);		
		argsMap.put("--arrayOOBPolicy", "assertions");
		
		argsMap.put("--heapsize", "10");
		argsMap.put("--intbits", "32");
		
		argsMap.put("--outputdir", "output/");
		argsMap.put("--tmpdir", tmpDirStr);
		argsMap.put("--keeptmpfiles", null);
		
//		argsMap.put("--verbosity", "4");
//		argsMap.put("--showphase", "lowering");
//		argsMap.put("--trace", null);
		argsMap.put(inputPath, null);
		
		return argsMap;
	}
	
	public void runOneTest(String[] args) throws IOException, InterruptedException {
		StencilSmtSketchMain main = new StencilSmtSketchMain(args);
		assertTrue(main.runBeforeGenerateCode());
		oracle = (SmtValueOracle) main.getOracle();
		stat = main.getSolutionStat();
	}
	
	@After
	public void printTiming() {
		System.out.println("\t" + stat.getSolutionTimeMs() + "\t" + stat.getIterations());
	}
	
	@Test
	public void miniTest0() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest0.sk");
		runOneTest(toArgArray(argsMap));
	}
	
	@Test
	public void miniTest1() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest1.sk");
		
		runOneTest(toArgArray(argsMap));
	}
	
	@Test
	public void miniTest2() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest2.sk");
		
		runOneTest(toArgArray(argsMap));
		// takes a long time to fail
	}
	
	@Test
	public void miniTest3() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest3.sk");
		runOneTest(toArgArray(argsMap));
	}
	
	@Test
	public void miniTest4() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest4.sk");
		runOneTest(toArgArray(argsMap));
	}
	
	@Test
	public void miniTest5() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest5.sk");
		
		runOneTest(toArgArray(argsMap));
	}
	
	@Test
	public void miniTest6() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest6.sk");
		
		runOneTest(toArgArray(argsMap));	
	}
	
	@Test
	public void miniTest7() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest7.sk");
		
		runOneTest(toArgArray(argsMap));
	}
	
	@Test
	public void miniTest8() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest8.sk");
		
		runOneTest(toArgArray(argsMap));
		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue(sorter.getHoleValueByOrder(0) == 1);
		assertTrue(sorter.getHoleValueByOrder(1) == 1);
	}
	
	@Test
	public void miniTest9() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest9.sk");
		
		runOneTest(toArgArray(argsMap));
	}
	
	@Test
	public void miniTest10() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest10.sk");
		
		runOneTest(toArgArray(argsMap));
	}
	
	@Test
	public void miniTest11() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest11.sk");
		
		runOneTest(toArgArray(argsMap));
	}
	
	@Test
	public void miniTest12() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest12.sk");
		
		runOneTest(toArgArray(argsMap));
	}
	
	@Test
	public void miniTest13() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest12.sk");
		
		runOneTest(toArgArray(argsMap));	
	}
	
	@Test
	public void miniTest14() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest14.sk");
		runOneTest(toArgArray(argsMap));
	}
	
	@Test
	public void miniTest15() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest15.sk");
		
		runOneTest(toArgArray(argsMap));	
	}
	
	@Test
	public void miniTest16() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest16.sk");
		
		runOneTest(toArgArray(argsMap));
	}
	
	@Test
	public void miniTest17() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest17.sk");
		runOneTest(toArgArray(argsMap));
	}
	
	@Test
	public void miniTest18() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest18.sk");
		runOneTest(toArgArray(argsMap));
	}
	
	@Test
	public void miniTest19() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest19.sk");
		
		runOneTest(toArgArray(argsMap));
	}
	
	@Test
	public void miniTest20() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest20.sk");
		
		runOneTest(toArgArray(argsMap));
	}
	
	@Test
	public void miniTest21() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest21.sk");
		
		runOneTest(toArgArray(argsMap));
	}
	
	@Test
	public void miniTest22() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest22.sk");
		
		runOneTest(toArgArray(argsMap));
	}
	
	@Test
	public void miniTest23() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest23.sk");
		
		runOneTest(toArgArray(argsMap));	
	}
	
	@Test
	public void miniTest24() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest24.sk");
		
		runOneTest(toArgArray(argsMap));
	}
	
	@Test
	public void miniTest25() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest25.sk");
		
		runOneTest(toArgArray(argsMap));
		// heap memory exhausted
	}	
	
	@Test
	public void miniTest26() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest26.sk");
		
		runOneTest(toArgArray(argsMap));
	}
	
	@Test
	public void miniTest27() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest27.sk");
		
		runOneTest(toArgArray(argsMap));
	}
	
	@Test
	public void miniTest29() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest29.sk");
		runOneTest(toArgArray(argsMap));
	}
	
	@Test
	public void miniTest28() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest28.sk");
		runOneTest(toArgArray(argsMap));
		// takes a long time to finish. like 10 mins
	}
	
	

}
