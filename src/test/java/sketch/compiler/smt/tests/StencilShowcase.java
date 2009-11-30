package sketch.compiler.smt.tests;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashMap;

import org.junit.After;

import sketch.compiler.main.sten.StencilSmtSketchMain;
import sketch.compiler.smt.partialeval.SmtValueOracle;

public class StencilShowcase extends sketch.compiler.seq.StencilShowcase {

	protected HashMap<String, String> initCmdArgs(String input) {
		String inputPath = "src/test/sk/smt/stenShowcase/" + input;
	
		HashMap<String, String> argsMap = new HashMap<String, String>();

		argsMap.put("--smtpath", System.getenv("smtpath"));
		
		argsMap.put("--bv", null);
		argsMap.put("--canon", null);
		argsMap.put("--funchash", null);
//		argsMap.put("--arrayOOBPolicy", "assertions");

//		argsMap.put("--heapsize", "10");
		argsMap.put("--intbits", "10");

		argsMap.put("--outputdir", "output/");
		argsMap.put("--tmpdir", tmpDirStr);
		argsMap.put("--keeptmpfiles", null);

		
		// argsMap.put("--trace", null);
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
	
	
	
	
	
}
