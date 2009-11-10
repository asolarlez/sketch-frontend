package sketch.compiler.smt.yices;

public class RegularExprTOABV extends RegularExprBlastBV {

	protected String[] initCmdArgs(String inputPath) {
		String[] args = {"--smtpath", System.getenv("smtpath"),
				"--backend", "yices",
				"--theoryofarray",
				"--bv",
				"--heapsize", "10",
				"--inbits", "32",
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
