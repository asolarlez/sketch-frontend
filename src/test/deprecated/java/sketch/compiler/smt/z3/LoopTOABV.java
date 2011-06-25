package sketch.compiler.smt.z3;

public class LoopTOABV extends sketch.compiler.smt.cvc3.LoopBlastBV {
	@Override
	protected String[] initCmdArgs(String inputPath) {
		String[] args = {"--smtpath", System.getenv("smtpath"),
				"--theoryofarray",
				"--bv",
				"--backend", "z3",
				"--arrayOOBPolicy", "assertions",
				"--heapsize", "10",
				"--keeptmpfiles",
				"--verbosity", "0",
				"--tmpdir", System.getenv("tmpdir"),
		"--outputdir", "output/",
		inputPath,
//		"--trace",
//		"--fakesolver",
		};
		
		return args;
	}
}
