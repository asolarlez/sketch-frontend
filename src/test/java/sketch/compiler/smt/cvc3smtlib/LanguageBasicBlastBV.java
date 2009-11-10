package sketch.compiler.smt.cvc3smtlib;

import java.util.HashMap;

public class LanguageBasicBlastBV extends sketch.compiler.smt.cvc3.LanguageBasicBlastBV {
	
	@Override
	protected HashMap<String, String> initCmdArgs(String inputPath) {
		HashMap<String, String> argsMap = new HashMap<String, String>();
		
		argsMap.put("--smtpath", System.getenv("smtpath"));
		argsMap.put("--backend", "cvc3smtlib");
		argsMap.put("--arrayOOBPolicy", "assertions");
		argsMap.put("--heapsize", "10");
		argsMap.put("--keeptmpfiles", null);
		argsMap.put("--verbosity", "0");
		argsMap.put("--tmpdir", "/tmp/sketch");
		argsMap.put("--outputdir", "output/");
//		argsMap.put("--verbosity", "4");
		argsMap.put(inputPath, null);
//		argsMap.put("--trace", null);
		
		return argsMap;
	}


}
