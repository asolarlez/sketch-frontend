package sketch.compiler.smt.sbit;

import java.io.IOException;
import java.util.HashMap;

import org.junit.After;

import sketch.compiler.main.seq.SequentialSketchMain;
import sketch.compiler.solvers.SolutionStatistics;

public class SketchRegressionTests extends
		sketch.compiler.seq.SketchRegressionTests {

	SolutionStatistics stat;
	
	
	@Override
	protected HashMap<String, String> initCmdArgsWithFileName(String input) {
	    String inputPath = "src/test/sk/seq/" + input;
		HashMap<String, String> argsMap = new HashMap<String, String>();
		
		argsMap.put("--sbitpath", System.getenv("sbitpath"));
		argsMap.put("--arrayOOBPolicy", "assertions");

		argsMap.put("--heapsize", "10");
//		argsMap.put("--inbits", "5");

		argsMap.put("--verbosity", "0");
		argsMap.put("--outputdir", "output/");
		argsMap.put("--output", "/tmp/" + input + ".tmp"); 
		argsMap.put("--keeptmpfiles", null);
		
		argsMap.put(inputPath, null);
		
		
		System.out.print(input.substring(input.lastIndexOf("\"")) + "\tSBit");
		
		return argsMap;
	}
	
	@Override
	public void runOneTest(String[] args) throws IOException,
			InterruptedException {
		SequentialSketchMain main = new SequentialSketchMain(args);
		main.parseProgram();
		//dump (prog, "After parsing:");
		main.preprocAndSemanticCheck();
		
		stat = main.partialEvalAndSolve();
		
	}
	
	@After
	public void printTiming() {
		if (stat == null)
			System.out.println("\tFAILED");
		else
			System.out.println("\t" + stat.elapsedTimeMs());
	}

    @Override
    protected HashMap<String, String> initCmdArgs(String string) {
        // NO OP
        return null;
    }

}
