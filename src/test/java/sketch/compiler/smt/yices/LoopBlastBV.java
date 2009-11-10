package sketch.compiler.smt.yices;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import sketch.compiler.main.seq.SequentialSMTSketchMain;

public class LoopBlastBV extends sketch.compiler.smt.cvc3.LoopBlastBV {

	protected String[] initCmdArgs(String inputPath) {
		String[] args = {"--smtpath", System.getenv("smtpath"),
				"--backend", "yices",
				"--bv",
				"--heapsize", "10",
				"--unrollamnt", "4",
				"--keeptmpfiles",
				"--verbosity", "0",
		"--tmpdir", "/tmp/sketch",
		"--outputdir", "output/",
		inputPath,
//		"--trace"
		};
		
		return args;
	}

	@Test
	public void testObjectWhileReal() throws Exception {
		String[] args = initCmdArgs("inputs/loop/objectWhileReal.sk");
		
		SequentialSMTSketchMain main = new SequentialSMTSketchMain(args);
		assertTrue(main.runBeforeGenerateCode());

		// because the hole values can be partial-evaluted away, they aren't
		// free variables to yices and thus yices doesn't output them. There
		// is no way for us to check for them.
	}
	
	@Test
	public void testObjectWhileSimulation() throws Exception {
		String[] args = initCmdArgs("inputs/loop/objectWhileSimulation.sk");
		
		SequentialSMTSketchMain main = new SequentialSMTSketchMain(args);
		assertTrue(main.runBeforeGenerateCode());
		
		// because the hole values can be partial-evaluted away, they aren't
		// free variables to yices and thus yices doesn't output them. There
		// is no way for us to check for them.
		
	}
	
	@Test
	public void testReversalUnrolled() throws Exception {
		String[] args = initCmdArgs("inputs/loop/reversalUnrolled.sk");
		
		SequentialSMTSketchMain main = new SequentialSMTSketchMain(args);
		assertTrue(main.runBeforeGenerateCode());
		
		// there is no hole in this test.
		// just serve as a preliminary test for LinkedListReversal benchmark,
		// which contains hole
		
	}
}
