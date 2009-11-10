package sketch.compiler.smt.cvc3;

public class RegularExprTOABV extends RegularExprTOAInt {
	
	protected String[] initCmdArgs(String inputPath) {
		String[] args = {"--smtpath", System.getenv("smtpath"),
				"--heapsize", "10",
				"--inbits", "32",
				"--keeptmpfiles",
				"--theoryofarray",
				"--bv",
				"--tmpdir", "/tmp/sketch",
				"--verbosity", "0",
		"--outputdir", "output/",
		inputPath,
//		"--trace"
		};
		
		return args;
	}

}
