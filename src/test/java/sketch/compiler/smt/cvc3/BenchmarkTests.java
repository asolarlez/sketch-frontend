package sketch.compiler.smt.cvc3;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;

import sketch.compiler.CommandLineParamManager;
import sketch.compiler.main.seq.SequentialSMTSketchMain;
import sketch.compiler.smt.HoleSorter;
import sketch.compiler.smt.SmtOracle;

/**
 * BenchmarkTests is a set of harder tests
 * 
 * @author Lexin Shan
 * @email lshan@eecs.berkeley.edu
 *
 */
public class BenchmarkTests {
	protected String[] initCmdArgs(String inputPath) {
		String[] args = {"--smtpath", System.getenv("smtpath"),
				"--heapsize", "10",
				"--keeptmpfiles",
		"--output", "/tmp/sketch2cvc.tmp",
		"--outputdir", "output/",
		"--unrollamnt", "20",
		inputPath,
//		"--trace",
		};
		
		return args;
	}
	
	@After
	public void cleanup() {
		CommandLineParamManager.getParams().clear();
	}
	
	@Test
	public void testFibonacci() throws Exception {
		String[] args = initCmdArgs("inputs/benchmarks/Fibonacci.sk");

		SequentialSMTSketchMain main = new SequentialSMTSketchMain(args);
		main.runBeforeGenerateCode();
		SmtOracle oracle = (Cvc3Oracle) main.getOracle();
		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue(sorter.getHoleValueByOrder(0) == 0);
		assertTrue(sorter.getHoleValueByOrder(1) == 1);
	}
}
