package sketch.compiler.smt.z3;

import java.util.HashMap;

public class RegularExprBlastBV extends sketch.compiler.smt.tests.RegularExprBlastBV{

	protected HashMap<String, String> initCmdArgs(String input) {
		HashMap<String, String> argsMap = super.initCmdArgs(input);
		argsMap.put("--backend", "z3");
		
//		 argsMap.put("--verbosity", "4");
//		 argsMap.put("--showphase", "lowering");
		System.out.print(input + "\tstp");
		return argsMap;
	}
}
