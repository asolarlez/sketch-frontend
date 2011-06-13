package sketch.compiler.smt.cvc3;

import java.util.HashMap;


/**
 * Cvc3, Theory Of Array, BitVector based
 * @author Lexin Shan
 * @email lshan@eecs.berkeley.edu
 *
 */
public class LanguageBasicTOABV extends LanguageBasicBlastBV {

	@Override
	protected HashMap<String, String> initCmdArgs(String inputPath) {
		HashMap<String, String> argsMap = new HashMap<String, String>();
		argsMap.put("--smtpath", System.getenv("smtpath"));
		argsMap.put("--backend", "cvc3");
		argsMap.put("--theoryofarray", null);
		argsMap.put("--arrayOOBPolicy", "assertions");
		argsMap.put("--bv", null);
		argsMap.put("--heapsize", "10");
		argsMap.put("--inbits", "32");
		argsMap.put("--cbits", "32");
		argsMap.put("--keeptmpfiles", null);
		argsMap.put("--verbosity", "0");
		argsMap.put("--outputdir", "output/");
		argsMap.put("--tmpdir", tmpDirStr);
		argsMap.put(inputPath, null);
//		argsMap.put("--trace", null);
		
		return argsMap;
	}
	
	
}
