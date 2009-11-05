package sketch.compiler.smt.yices;

import sketch.compiler.smt.yices.LoopBlastBV;

public class LoopTOABV extends LoopBlastBV {
	
	protected String[] initCmdArgs(String inputPath) {
		String[] args = {"--smtpath", System.getenv("smtpath"),
				"--backend", "yices",
				"--theoryofarray",
				"--bv",
				"--heapsize", "10",
				"--unrollamnt", "4",
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
