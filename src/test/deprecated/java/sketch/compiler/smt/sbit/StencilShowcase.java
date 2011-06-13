package sketch.compiler.smt.sbit;

import java.io.IOException;
import java.util.HashMap;

import org.junit.After;

import sketch.compiler.main.sten.StencilSketchMain;
import sketch.compiler.solvers.SolutionStatistics;

public class StencilShowcase extends sketch.compiler.seq.StencilShowcase {

	SolutionStatistics stat;
	
	@Override
	protected HashMap<String, String> initCmdArgs(String input) {
		String inputPath = "src/test/sk/smt/stenShowcase/" + input;
		
		HashMap<String, String> argsMap = new HashMap<String, String>();
		
//		argsMap.put("--sbitpath", System.getenv("sbitpath"));
		argsMap.put("--arrayOOBPolicy", "assertions");

		argsMap.put("--heapsize", "10");
//		argsMap.put("--inbits", "5");

		argsMap.put("--verbosity", "0");
		argsMap.put("--outputdir", "output/");
		argsMap.put("--output", "/tmp/" + input + ".tmp"); 
		argsMap.put("--keeptmpfiles", null);
		argsMap.put("--timeout", "30");
		
		argsMap.put(inputPath, null);
		
		
		System.out.print(input + "\tSBit");
		
		return argsMap;
	}

	@Override
	public void runOneTest(String[] args) throws IOException,
			InterruptedException {
	    StencilSketchMain main = new StencilSketchMain(args);
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
	

}
