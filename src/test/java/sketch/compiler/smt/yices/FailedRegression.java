package sketch.compiler.smt.yices;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

import sketch.compiler.main.seq.SequentialSMTSketchMain;
import sketch.compiler.smt.HoleSorter;
import sketch.compiler.smt.SmtOracle;
import sketch.compiler.smt.TestHarness;
import sketch.compiler.smt.partialeval.NodeToSmtValue;

public class FailedRegression extends TestHarness{

	protected String[] initCmdArgs(String inputPath) {
		System.out.println("Testing " +  inputPath);
		String[] args = {"--smtpath", System.getenv("smtpath"),
				"--backend", "yices",
				"--bv",
				"--arrayOOBPolicy", "assertions",
				"--heapsize", "10",
				"--keeptmpfiles",
				"--tmpdir", "/tmp/sketch",
				"--verbosity", "4",
				"--cbits", "32",
		"--outputdir", "output/",
		inputPath,
//		"--trace",
//		"--fakesolver",
		"--showphase", "lowering"
		};
		
		return args;
	}
	
//	@Test
//	public void miniTest8() throws IOException, InterruptedException {
//		String[] args = initCmdArgs("inputs/stenTests/regtest/miniTest8.sk");
//		
//		StencilSmtSketchMain main = new StencilSmtSketchMain(args);
//		assertTrue(main.runBeforeGenerateCode());
//
//		SmtValueOracle mOutputOracle = (SmtValueOracle) main.getOracle();
//		HoleSorter sorter = new HoleSorter(mOutputOracle);
//		assertTrue(sorter.getHoleValueByOrder(0) == 1);
//		assertTrue(sorter.getHoleValueByOrder(1) == 1);
//		// verification takes a long time
//		// tried running it over 1.5 hours. still not finished.
//	}
//	
//	@Test
//	public void miniTestb101() throws IOException, InterruptedException {
//		String[] args = initCmdArgs("inputs/sketchTests/regtests/miniTestb101.sk");
//
//			SequentialSMTSketchMain main = new SequentialSMTSketchMain(args);
//			assertTrue(main.runBeforeGenerateCode());
//
//			SmtOracle<NodeToSmtValue> mOutputOracle = (SmtOracle<NodeToSmtValue>) main.getOracle();
//			// uninterpreted function
//	}
//	@Test
//	public void miniTestb120() throws IOException, InterruptedException {
//		String[] args = initCmdArgs("inputs/sketchTests/regtests/miniTestb120.sk");
//
//			SequentialSMTSketchMain main = new SequentialSMTSketchMain(args);
//			assertTrue(main.runBeforeGenerateCode());
//
//			SmtOracle<NodeToSmtValue> mOutputOracle = (SmtOracle<NodeToSmtValue>) main.getOracle();
//			
//			// infinite loop
//			// verifier is not finding a "good" counterexample. 
//	}
//	
//	@Test
//	public void miniTest49() throws IOException, InterruptedException {
//		String[] args = initCmdArgs("inputs/sketchTests/regtests/miniTest49.sk");
//		
//		SequentialSMTSketchMain main = new SequentialSMTSketchMain(args);
//		assertTrue(main.runBeforeGenerateCode());
//		
//		SmtOracle<NodeToSmtValue> mOutputOracle = (SmtOracle<NodeToSmtValue>) main.getOracle();
//		HoleSorter sorter = new HoleSorter(mOutputOracle); 
//		
//		//mod 
//	}
//	
//	@Test
//	public void miniTest50() throws IOException, InterruptedException {
//		String[] args = initCmdArgs("inputs/sketchTests/regtests/miniTest50.sk");
//		
//		SequentialSMTSketchMain main = new SequentialSMTSketchMain(args);
//		assertTrue(main.runBeforeGenerateCode());
//		
//		SmtOracle<NodeToSmtValue> mOutputOracle = (SmtOracle<NodeToSmtValue>) main.getOracle();
//		HoleSorter sorter = new HoleSorter(mOutputOracle); 
//		
//		// mod 
//	}
//	
//	@Test
//	public void miniTest52() throws IOException, InterruptedException {
//		String[] args = initCmdArgs("inputs/sketchTests/regtests/miniTest52.sk");
//		
//		SequentialSMTSketchMain main = new SequentialSMTSketchMain(args);
//		assertTrue(main.runBeforeGenerateCode());
//		
//		SmtOracle<NodeToSmtValue> mOutputOracle = (SmtOracle<NodeToSmtValue>) main.getOracle();
//		HoleSorter sorter = new HoleSorter(mOutputOracle); 
//		
//		// mod
//	}
//	@Test
//	public void miniTest68() throws IOException, InterruptedException {
//		String[] args = initCmdArgs("inputs/sketchTests/regtests/miniTest68.sk");
//
//		SequentialSMTSketchMain main = new SequentialSMTSketchMain(args);
//		assertTrue(main.runBeforeGenerateCode());
//
//		SmtValueOracle mOutputOracle = (SmtValueOracle) main.getOracle();	
//		// mod
//	}
//	
//	@Test
//	public void miniTest88() throws IOException, InterruptedException {
//		String[] args = initCmdArgs("inputs/sketchTests/regtests/miniTest88.sk");
//
//			SequentialSMTSketchMain main = new SequentialSMTSketchMain(args);
//			assertTrue(main.runBeforeGenerateCode());
//
//			SmtOracle<NodeToSmtValue> mOutputOracle = (SmtOracle<NodeToSmtValue>) main.getOracle();
//			// mod
//	}
//	
//	@Test
//	public void miniTestb160() throws IOException, InterruptedException {
//		String[] args = initCmdArgs("inputs/sketchTests/regtests/miniTestb160.sk");
//
//			SequentialSMTSketchMain main = new SequentialSMTSketchMain(args);
//			assertTrue(main.runBeforeGenerateCode());
//
//			SmtOracle<NodeToSmtValue> mOutputOracle = (SmtOracle<NodeToSmtValue>) main.getOracle();
//			// mod
//	}
//	@Test
//	public void miniTestb161() throws IOException, InterruptedException {
//		String[] args = initCmdArgs("inputs/sketchTests/regtests/miniTestb161.sk");
//
//			SequentialSMTSketchMain main = new SequentialSMTSketchMain(args);
//			assertTrue(main.runBeforeGenerateCode());
//
//			SmtOracle<NodeToSmtValue> mOutputOracle = (SmtOracle<NodeToSmtValue>) main.getOracle();
//			// mod
//	}
//	
//	@Test
//	public void miniTest93() throws IOException, InterruptedException {
//		String[] args = initCmdArgs("inputs/sketchTests/regtests/miniTest93.sk");
//
//			SequentialSMTSketchMain main = new SequentialSMTSketchMain(args);
//			assertTrue(main.runBeforeGenerateCode());
//			// uninterpreted function
//	}
//	
//	@Test
//	public void miniTestb105() throws IOException, InterruptedException {
//		String[] args = initCmdArgs("inputs/sketchTests/regtests/miniTestb105.sk");
//
//			SequentialSMTSketchMain main = new SequentialSMTSketchMain(args);
//			assertTrue(main.runBeforeGenerateCode());
//
//			// infinite CEGIS loop b/c of adverse sketch
//			
//			SmtOracle<NodeToSmtValue> mOutputOracle = (SmtOracle<NodeToSmtValue>) main.getOracle();
//			HoleSorter sorter = new HoleSorter(mOutputOracle);
//			assertTrue((sorter.getHoleValueByOrder(0) == 1));
//	}
//	
//	@Test
//	public void miniTestb106() throws IOException, InterruptedException {
//		String[] args = initCmdArgs("inputs/sketchTests/regtests/miniTestb106.sk");
//
//			SequentialSMTSketchMain main = new SequentialSMTSketchMain(args);
//			assertTrue(main.runBeforeGenerateCode());
//			
//			// infinite CEGIS loop b/c of adverse sketch
//			SmtOracle<NodeToSmtValue> mOutputOracle = (SmtOracle<NodeToSmtValue>) main.getOracle();
//			HoleSorter sorter = new HoleSorter(mOutputOracle);
//			assertTrue((sorter.getHoleValueByOrder(0) == 3));
//	}
}
