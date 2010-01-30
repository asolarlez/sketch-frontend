package sketch.compiler.smt.cvc3;

import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;

import sketch.compiler.main.seq.SequentialSMTSketchMain;
import sketch.compiler.main.seq.SequentialSketchOptions;
import sketch.compiler.smt.HoleSorter;
import sketch.compiler.smt.SmtOracle;

public class LoopTOAInt {

	protected String[] initCmdArgs(String inputPath) {
		String[] args = {"--smtpath", System.getenv("smtpath"),
				"--heapsize", "10",
				"--unrollamnt", "4",
				"--theoryofarray",
				"--keeptmpfiles",
				"--verbosity", "0",
		"--tmpdir", "/tmp/sketch",
		"--outputdir", "output/",
		inputPath,
//		"--trace"
		};
		
		return args;
	}
	
	@After
	public void cleanup() {
	    SequentialSketchOptions.resetSingleton();
	}
	
	
	@Test
	public void testObjectWhileReal() throws Exception {
		String[] args = initCmdArgs("inputs/loop/objectWhileReal.sk");
		
		SequentialSMTSketchMain main = new SequentialSMTSketchMain(args);
		assertTrue(main.runBeforeGenerateCode());
		SmtOracle oracle = (SmtOracle) main.getOracle();
		
		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue(sorter.getHoleValueByOrder(0) == 2);		
		assertTrue(sorter.getHoleValueByOrder(1) == 3);
		
	}
	
	@Test
	public void testObjectWhileSimulation() throws Exception {
		String[] args = initCmdArgs("inputs/loop/objectWhileSimulation.sk");
		
		SequentialSMTSketchMain main = new SequentialSMTSketchMain(args);
		assertTrue(main.runBeforeGenerateCode());
		SmtOracle oracle = (SmtOracle) main.getOracle();
		
		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue(sorter.getHoleValueByOrder(0) == 2);		
		assertTrue(sorter.getHoleValueByOrder(1) == 3);
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
