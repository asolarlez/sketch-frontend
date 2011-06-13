package sketch.compiler.smt.cvc3;

import java.util.HashMap;

/**
 * Cvc3 Blast BitVector
 * @author Lexin Shan
 * @email lshan@eecs.berkeley.edu
 *
 */
public class LanguageBasicBlastBV extends sketch.compiler.smt.tests.LanguageBasicBlastBV {
	
    protected HashMap<String, String> initCmdArgs(String input) {
        HashMap<String, String> argsMap = super.initCmdArgs(input);
        argsMap.put("--backend", "cvc3");
        
        argsMap.put("--verbosity", "0");
//        argsMap.put("--showphase", "lowering");
        System.out.print(input + "\tcvc3");
        return argsMap;
    }
	

}
