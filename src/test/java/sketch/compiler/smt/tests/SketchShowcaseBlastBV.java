package sketch.compiler.smt.tests;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashMap;

import org.junit.After;

import sketch.compiler.main.seq.SequentialSMTSketchMain;
import sketch.compiler.smt.SolverFailedException;
import sketch.compiler.smt.partialeval.SmtValueOracle;

public class SketchShowcaseBlastBV extends sketch.compiler.seq.SketchShowcase {

    protected String mStatus;
    
	@Override
	protected HashMap<String, String> initCmdArgs(String input) {
		String inputPath = "src/test/sk/smt/sketchTests/showcase/" + input;
	
		HashMap<String, String> argsMap = new HashMap<String, String>();

		argsMap.put("--smtpath", System.getenv("smtpath"));
		
		argsMap.put("--modelint", "bv");
		argsMap.put("--uselet", null);
//		argsMap.put("--arrayOOBPolicy", "assertions");

		argsMap.put("--heapsize", "10");
		argsMap.put("--intbits", "5");

		argsMap.put("--outputdir", "output/");
		argsMap.put("--tmpdir", tmpDirStr);
		argsMap.put("--keeptmpfiles", null);
		argsMap.put("--timeout", "10");
		
		// argsMap.put("--trace", null);
		argsMap.put(inputPath, null);

		return argsMap;
	}

	@Override
	public void runOneTest(String[] args) throws IOException, InterruptedException {
		oracle = null;
		stat = null;
		try {
    		SequentialSMTSketchMain main = new SequentialSMTSketchMain(args);
    		assertTrue(main.runBeforeGenerateCode());
    		oracle = (SmtValueOracle) main.getOracle();
            stat = main.getSolutionStat();
		} catch (SolverFailedException e) {
		    mStatus = "NO_MODEL";
		    stat = null;
		}
		
	}
	
	@After
	public void printTiming() {
		if (stat == null)
			System.out.println("\t" + mStatus);
		else
			System.out.println("\t" + stat.getSolutionTimeMs() + "\t" + stat.getIterations());
	}
	

}
