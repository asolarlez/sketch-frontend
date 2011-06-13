package sketch.compiler.smt.stp;

import java.util.HashMap;

public class FailedStencilRegression extends sketch.compiler.smt.yices.FailedStencilRegression {
	protected HashMap<String, String> initCmdArgs(String input) {
		String inputPath = "inputs/stenTests/regtest/" + input;
		
		System.out.print(input + "\tSTP");
		
		HashMap<String, String> argsMap = new HashMap<String, String>();
		
		argsMap.put("--smtpath", System.getenv("smtpath"));
		argsMap.put("--backend", "stp");
		argsMap.put("--bv", null);		
		argsMap.put("--arrayOOBPolicy", "assertions");
		
		argsMap.put("--heapsize", "10");
		argsMap.put("--intbits", "4");
		
		argsMap.put("--outputdir", "output/");
		argsMap.put("--tmpdir", tmpDirStr);
		argsMap.put("--keeptmpfiles", null);
		
//		argsMap.put("--verbosity", "4");
//		argsMap.put("--showphase", "parse");
//		argsMap.put("--trace", null);
		argsMap.put(inputPath, null);
		
		return argsMap;
	}
}
