package sketch.compiler.smt.z3;

import java.util.HashMap;

public class LanguageBasicTOABV extends sketch.compiler.smt.cvc3smtlib.LanguageBasicBlastBV  {
	@Override
	protected HashMap<String, String> initCmdArgs(String inputPath) {
		HashMap<String, String> argsMap = new HashMap<String, String>();
		
		argsMap.put("--smtpath", System.getenv("smtpath"));
		argsMap.put("--backend", "z3");
		argsMap.put("--bv", null);
		argsMap.put("--theoryofarray", null);
		argsMap.put("--arrayOOBPolicy", "assertions");
		
		argsMap.put("--heapsize", "10");
		argsMap.put("--inbits", "32");
		
		argsMap.put("--outputdir", "output/");
		argsMap.put("--tmpdir", tmpDirStr);
		argsMap.put("--keeptmpfiles", null);
		
		argsMap.put("--verbosity", "0");
//		argsMap.put("--showphase", "lowering");
//		argsMap.put("--trace", null);
		argsMap.put(inputPath, null);
		
		return argsMap;
	}
}
