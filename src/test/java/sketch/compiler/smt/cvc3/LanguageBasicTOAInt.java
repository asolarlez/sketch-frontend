package sketch.compiler.smt.cvc3;

import java.io.File;
import java.util.HashMap;

/**
 * LanguageBasicTOABV is a set of tests that cover the basic language
 * constructs
 * 
 * Cvc3 Theory Of Array Integer
 * 
 * @author Lexin Shan
 * @email lshan@eecs.berkeley.edu
 *
 */

public class LanguageBasicTOAInt extends LanguageBasicBlastBV {
	
	protected final static String tmpDirStr = "/tmp/sketch";
	protected File tmpDir = new File(tmpDirStr);
	
	protected HashMap<String, String> initCmdArgs(String inputPath) {
		HashMap<String, String> argsMap = new HashMap<String, String>();
		argsMap.put("--smtpath", System.getenv("smtpath"));
		argsMap.put("--theoryofarray", null);
		argsMap.put("--arrayOOBPolicy", "assertions");
		argsMap.put("--heapsize", "10");
		argsMap.put("--keeptmpfiles", null);
		argsMap.put("--verbosity", "0");
		argsMap.put("--outputdir", "output/");
		argsMap.put("--tmpdir", tmpDirStr);
		argsMap.put(inputPath, null);
//		argsMap.put("--trace", null);
//		argsMap.put("--showphase", "preproc");
		
		return argsMap;
	}
	

}
