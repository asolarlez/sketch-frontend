package sketch.compiler.smt.cvc3;

public class LoopTOABV extends LoopTOAInt {
	
	protected String[] initCmdArgs(String inputPath) {
		String[] args = {"--smtpath", System.getenv("smtpath"),
				"--heapsize", "10",
				"--unrollamnt", "4",
				"--theoryofarray",
				"--bv",
				"--keeptmpfiles",
				"--verbosity", "0",
		"--tmpdir", "/tmp/sketch",
		"--outputdir", "output/",
		inputPath,
//		"--trace"
		};
		
		return args;
	}

}
