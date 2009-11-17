package sketch.compiler.smt.yices;

import java.util.HashMap;

public class BitVectorBlastBV extends sketch.compiler.smt.tests.BitVectorBlastBV {

	protected HashMap<String, String> initCmdArgs(String input) {
		HashMap<String, String> argsMap = super.initCmdArgs(input);
		argsMap.put("--backend", "yices2");
		
		// argsMap.put("--verbosity", "4");
		// argsMap.put("--showphase", "parse");
		System.out.print(input + "\tyices");
		return argsMap;
	}
}
