package sketch.compiler.smt.cvc3;


/**
 * Tests the cases where a regular expression is involved in a condition
 * if (a < {|x|y|}) ...
 * if ({|x|y|} < a) ...
 * if ({|a|b|} < {|x|y|}) ...
 * @author Lexin Shan
 * @email lshan@eecs.berkeley.edu
 *
 */
public class RegularExprTOAInt {
	
	protected String[] initCmdArgs(String inputPath) {
		String[] args = {"--smtpath", System.getenv("smtpath"),
				"--theoryofarray",
				"--heapsize", "10",
				"--keeptmpfiles",
				"--verbosity", "0",
		"--tmpdir", "/tmp/sketch",
		"--outputdir", "output/",
		inputPath,
//		"--trace",
//		"--showphase", "preproc",
		
		};
		
		return args;
	}
	

}
