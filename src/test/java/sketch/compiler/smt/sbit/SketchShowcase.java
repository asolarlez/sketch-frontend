package sketch.compiler.smt.sbit;

import java.io.IOException;
import java.util.HashMap;

import org.junit.After;

import sketch.compiler.main.seq.SequentialSketchMain;
import sketch.compiler.solvers.SolutionStatistics;

public class SketchShowcase extends sketch.compiler.seq.SketchShowcase {

	SolutionStatistics stat;
	@Override
	protected HashMap<String, String> initCmdArgs(String input) {
		String inputPath = "src/test/sk/smt/sketchTests/showcase/" + input;
		
		HashMap<String, String> argsMap = new HashMap<String, String>();
		
//		argsMap.put("--arrayOOBPolicy", "assertions");

		argsMap.put("--heapsize", "10");
//		argsMap.put("--inbits", "5");

		argsMap.put("--verbosity", "0");
		argsMap.put("--outputdir", "output/");
		argsMap.put("--output", tmpDirStr + "//" + input + ".tmp"); 
		argsMap.put("--keeptmpfiles", null);
		argsMap.put("--timeout", "10");
		
		argsMap.put(inputPath, null);
		
		System.out.print(input + "\tSBit");
		
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
			System.out.println("\t" + mResult);
		else
			System.out.println("\t" + stat.elapsedTimeMs());
	}
	
	String mResult;
	
	@Override
	public void SpMV_DIA1_N3_BIT() throws IOException, InterruptedException {
	    initCmdArgs("SpMV-DIA1-N3-BIT.sk");
        mResult = "TIMEOUT";
	}
	
	@Override
	public void SpMV_DIA1_N3_INT() throws IOException, InterruptedException {
	    initCmdArgs("SpMV-DIA1-N3-INT.sk");
        mResult = "TIMEOUT";
	}
	
	@Override
	public void SpMV_CSR1_N2_BIT() throws IOException, InterruptedException {
	    initCmdArgs("SpMV-CSR1-N2-BIT.sk");
	    mResult = "TIMEOUT";
	}

	@Override
	public void SpMV_CSR1_N2_INT() throws IOException, InterruptedException {
	    initCmdArgs("SpMV-CSR1-N2-INT.sk");
        mResult = "TIMEOUT";
	}
	
	@Override
	public void SpMV_CSR1_N3_BIT() throws IOException, InterruptedException {
	    initCmdArgs("SpMV-CSR1-N3-BIT.sk");
        mResult = "TIMEOUT";
	}
	
	@Override
	public void aesFullStage() throws IOException, InterruptedException {
	    initCmdArgs("aesFullStage.sk");
        mResult = "TIMEOUT";
	}
	
	@Override
	public void polynomialInt() throws IOException, InterruptedException {
	    initCmdArgs("polynomialInt.sk");
        mResult = "TIMEOUT";
	}
	
	
	
}
