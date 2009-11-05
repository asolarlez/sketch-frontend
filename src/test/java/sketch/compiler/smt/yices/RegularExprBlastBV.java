package sketch.compiler.smt.yices;

public class RegularExprBlastBV extends sketch.compiler.smt.cvc3.RegularExprBlastBV {

	protected String[] initCmdArgs(String inputPath) {
		String[] args = {"--smtpath", System.getenv("smtpath"),
				"--backend", "yices",
				"--bv",
				"--heapsize", "10",
				"--inbits", "32",
				"--keeptmpfiles",
				"--verbosity", "0",
		"--tmpdir", "/tmp/sketch",
		"--outputdir", "output/",
		inputPath,
//		"--trace",
//		"--showphase", "lowering",
		};
		
		return args;
	}
	
}
