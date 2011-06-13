package sketch.compiler.smt.beaver;

import java.util.HashMap;

public class LanguageBasicBlastBV extends sketch.compiler.smt.cvc3smtlib.LanguageBasicBlastBV  {
	@Override
	protected HashMap<String, String> initCmdArgs(String inputPath) {
		HashMap<String, String> argsMap = new HashMap<String, String>();
		argsMap.put("--smtpath", System.getenv("smtpath"));
		
		argsMap.put("--backend", "beaver");
		argsMap.put("--arrayOOBPolicy", "assertions");
		argsMap.put("--bv", null);
		
		argsMap.put("--heapsize", "10");
		argsMap.put("--inbits", "32");
		argsMap.put("--cbits", "32");
		
		argsMap.put("--keeptmpfiles", null);
		argsMap.put("--outputdir", "output/");
		argsMap.put("--tmpdir", tmpDirStr);
		
		argsMap.put("--verbosity", "0");
//		argsMap.put("--trace", null);
//		argsMap.put("--showphase", "lowering");
		
		argsMap.put(inputPath, null);
		
		return argsMap;
	}
}
