package sketch.compiler.smt.comp;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import sketch.compiler.main.seq.SequentialSketchMain;
import sketch.compiler.main.seq.SequentialSMTSketchMain;
import sketch.compiler.main.sten.StencilSketchMain;
import sketch.compiler.main.sten.StencilSmtSketchMain;
import sketch.compiler.smt.TestHarness;
import sketch.compiler.smt.CEGISLoop.CEGISStat;
import sketch.compiler.solvers.SolutionStatistics;

public abstract class Comparison extends TestHarness {

	protected static final String tmpDirStr = "C:\\Users\\lshan\\AppData\\Local\\Temp";
	protected File tmpDir = new File(tmpDirStr);

	public Comparison() {
		super();
	}

	protected abstract HashMap<String, String> initCmdArgs(String inputPath, String backend,
			String smtpath, boolean toa, boolean bv);

	protected void runSBit(String inputPath) {
		if (System.getenv("sbitpath") == null)
			throw new IllegalStateException("env var \"sbit\" needs to be set to an a dir that contains SBit.");
		String[] args = {
//				"--showphase", "lowering",
				"--sbitpath", System.getenv("sbitpath"), 
				"--verbosity", "0",
				"--unrollamnt", "4",
				"--heapsize", "10",
				"--arrayOOBPolicy", "assertions",
				"--output", "/tmp/sat.tmp", 
				"--outputdir", "output/",
				"--keeptmpfiles",
//				"--trace",
				inputPath,
				
		};
		SequentialSketchMain main = new SequentialSketchMain(args);
		main.parseProgram();
		//dump (prog, "After parsing:");
		main.preprocAndSemanticCheck();
		
		
		SolutionStatistics stat = main.partialEvalAndSolve();
		String programName = inputPath.substring(inputPath.lastIndexOf(File.separatorChar) + 1,
				inputPath.lastIndexOf('.'));
		
		System.out.println(programName + "\tSBit\t\t"
				+ stat.solutionTimeMs());
	}
	
	protected void runStencilSK(String inputPath) {
		if (System.getenv("sbitpath") == null)
			throw new IllegalStateException("env var \"sbit\" needs to be set to an a dir that contains SBit.");
		String[] args = {
//				"--showphase", "lowering",
				"--sbitpath", System.getenv("sbitpath"), 
				"--verbosity", "0",
				"--unrollamnt", "4",
				"--heapsize", "10",
				"--arrayOOBPolicy", "assertions",
				"--output", "/tmp/sat.tmp", 
				"--outputdir", "output/",
				"--keeptmpfiles",
//				"--trace",
				inputPath,
				
		};
		StencilSketchMain main = new StencilSketchMain(args);
		main.parseProgram();
		//dump (prog, "After parsing:");
		main.preprocAndSemanticCheck();
		
		
		SolutionStatistics stat = main.partialEvalAndSolve();
		String programName = inputPath.substring(inputPath.lastIndexOf(File.separatorChar) + 1,
				inputPath.lastIndexOf('.'));
		
		System.out.println(programName + "\tSBit\t\t"
				+ stat.solutionTimeMs());
	}
	
	protected void runToSmt(String name, String inputPath, String backend,
			boolean toa, boolean bv) throws IOException, InterruptedException {

		String smtpath = "";
		if (backend.equals("cvc3") || backend.equals("cvc3smtlib")) {
			smtpath = "/Users/lshan/Applications/bin/cvc3";
		} else if (backend.equals("yices")) {
			smtpath = "/Users/lshan/Applications/bin/yices";
		} else if (backend.equals("z3")) {
			smtpath = "C:\\Program Files\\Microsoft Research\\z3-1.3.6\\bin\\z3.exe";
			inputPath = inputPath.replace('/', File.separatorChar);
		}
		HashMap<String, String> argsMap = initCmdArgs(inputPath, backend, smtpath, toa, bv);

		SequentialSMTSketchMain main = new SequentialSMTSketchMain(toArgArray(argsMap));

		assertTrue(main.runBeforeGenerateCode());
		CEGISStat stat = main.getSolutionStat();
		System.out.println(main.getProgramName() + "\t" + backend + "\t" + name + "\t"
				+ stat.getSolutionTimeMs() + "\t" + stat.getIterations());
	}
	
	protected void runToSmtStencil(String name, String inputPath, String backend,
			boolean toa, boolean bv) throws IOException, InterruptedException {

		String smtpath = "";
		if (backend.equals("cvc3") || backend.equals("cvc3smtlib")) {
			smtpath = "/Users/lshan/Applications/bin/cvc3";
		} else if (backend.equals("yices")) {
			smtpath = "/Users/lshan/Applications/bin/yices";
		} else if (backend.equals("z3")) {
			smtpath = "C:\\Program Files\\Microsoft Research\\z3-1.3.6\\bin\\z3.exe";
			inputPath = inputPath.replace('/', File.separatorChar);
		}
		HashMap<String, String> argsMap = initCmdArgs(inputPath, backend, smtpath, toa, bv);

		StencilSmtSketchMain main = new StencilSmtSketchMain(toArgArray(argsMap));

		assertTrue(main.runBeforeGenerateCode());
		CEGISStat stat = main.getSolutionStat();
		System.out.println(main.getProgramName() + "\t" + backend + "\t" + name + "\t"
				+ stat.getSolutionTimeMs() + "\t" + stat.getIterations());
	}
	
}