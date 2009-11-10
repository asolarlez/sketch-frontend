package sketch.compiler.smt.tests;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashMap;

import org.junit.After;

import sketch.compiler.main.seq.SequentialSMTSketchMain;
import sketch.compiler.smt.partialeval.SmtValueOracle;

public class BinTree extends sketch.compiler.seq.BinTree {

	@Override
	protected HashMap<String, String> initCmdArgs(String input) {
		String inputPath = "inputs/comparisons/bintree/" + input;
	
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

	@Override
	protected void runOneTest(String[] args) throws IOException,
			InterruptedException {
		oracle = null;
		stat = null;
		SequentialSMTSketchMain main = new SequentialSMTSketchMain(args);
		assertTrue(main.runBeforeGenerateCode());
		oracle = (SmtValueOracle) main.getOracle();
		stat = main.getSolutionStat();
	}

	@After
	public void printTiming() {
		System.out.println("\t" + stat.getSolutionTimeMs());
	}
}
