package sketch.compiler.smt.comp;

import java.util.HashMap;

import org.junit.Test;

public class SketchRegComp extends Comparison {

	protected HashMap<String, String> initCmdArgs(String inputPath, String backend,
			String smtpath, boolean toa, boolean bv) {
		HashMap<String, String> argsMap = new HashMap<String, String>();
		
		argsMap.put("--smtpath", smtpath);
		argsMap.put("--backend", backend);
		
		if (toa)
			argsMap.put("--theoryofarray", null);
		if (bv) 
			argsMap.put("--bv", null);
		
		argsMap.put("--arrayOOBPolicy", "assertions");
		
		argsMap.put("--heapsize", "10");
		argsMap.put("--inbits", "32");
		argsMap.put("--intbits", "5");
		
		argsMap.put("--outputdir", "output/");
		argsMap.put("--tmpdir", tmpDirStr);
//		argsMap.put("--keeptmpfiles", null);
		
		argsMap.put("--verbosity", "0");
//		argsMap.put("--showphase", "lowering");
//		argsMap.put("--trace", null);
		argsMap.put(inputPath, null);
		
		return argsMap;
	}
	
//	protected String[] initCmdArgs(String inputPath, String backend,
//			String smtpath, boolean toa, boolean bv) {
//		ArrayList<String> args = new ArrayList<String>();
//		args.add("--smtpath");
//		args.add(smtpath);
//
//		args.add("--backend");
//		args.add(backend);
//		
//		if (toa)
//			args.add("--theoryofarray");
//		if (bv)
//			args.add("--bv");
//		
//		args.add("--inbits");
//		args.add("4");
//
//		args.add("--arrayOOBPolicy");
//		args.add("assertions");
//
//		args.add("--heapsize");
//		args.add("10");
//
//		args.add("--unrollamnt");
//		args.add("4");
//
////		args.add("--keeptmpfiles");
//
//		args.add("--outputdir");
//		args.add("output/");
//		args.add("--tmpdir");
//		args.add(tmpDirStr);
//		
//		args.add("--intbits");
//		args.add("32");
//		
//		args.add("--verbosity");
//		args.add("0");
//
////		args.add("--trace");
//		args.add(inputPath);
//
//		String[] dummy = new String[0];
//		dummy = args.toArray(dummy);
//		return dummy;
//	}
	
	protected void runOneTest(String input) {
		String inputPath = "inputs/sketchTests/regtests/" + input;
		runSBit(inputPath);
		cleanup();
		try {
			runToSmt("BV", inputPath, "yices", false, true);
		} catch (Exception e) {
			System.err.println("yices failed");
		}
	}
	
	@Test
	public void miniTest1() {
		String input = "miniTest1.sk";
		runOneTest(input);
	}
	
	@Test
	public void miniTest48() {
		String input = "miniTest48.sk";
		runOneTest(input);
	}
	
	@Test
	public void miniTest51() {
		String input = "miniTest51.sk";
		runOneTest(input);
	}
	
	@Test
	public void miniTest92() {
		String input = "miniTest92.sk";
		runOneTest(input);
	}
	
	@Test
	public void miniTest95() {
		String input = "miniTest95.sk";
		runOneTest(input);
	}
	
	@Test
	public void miniTestb102() {
		String input = "miniTestb102.sk";
		runOneTest(input);	
	}
	
	@Test
	public void miniTestb103() {
		String input = "miniTestb103.sk";
		runOneTest(input);
	}
	
	@Test
	public void miniTestb104() {
		String input = "miniTestb104.sk";
		runOneTest(input);
	}
	
	@Test
	public void miniTestb147() {
		String input = "miniTestb147.sk";
		runOneTest(input);
	}
	
	@Test
	public void miniTestb153() {
		String input = "miniTestb153.sk";
		runOneTest(input);
	}



}
