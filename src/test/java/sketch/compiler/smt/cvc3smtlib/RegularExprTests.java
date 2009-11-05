package sketch.compiler.smt.cvc3smtlib;

public class RegularExprTests extends sketch.compiler.smt.cvc3.RegularExprTOAInt{


	@Override
	protected String[] initCmdArgs(String inputPath) {
		String[] args = {"--smtpath", System.getenv("smtpath"),
				"--backend", "cvc3smtlib",
				"--arrayOOBPolicy", "assertions",
				"--heapsize", "10",
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
