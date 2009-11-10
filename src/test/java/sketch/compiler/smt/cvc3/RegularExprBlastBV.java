package sketch.compiler.smt.cvc3;

public class RegularExprBlastBV extends RegularExprBlastInt {

	protected String[] initCmdArgs(String inputPath) {
		String[] args = {"--smtpath", System.getenv("smtpath"),
				"--heapsize", "10",
				"--inbits", "32",
				"--keeptmpfiles",
				"--verbosity", "0",
		"--tmpdir", "/tmp/sketch",
		"--outputdir", "output/",
		inputPath,
//		"--trace"
//		"--showphase", "lowering",
		};
		
		return args;
	}
}
