package sketch.compiler.smt.yices;

import java.util.HashMap;

public class BinTree extends sketch.compiler.smt.tests.BinTree {

	@Override
	protected HashMap<String, String> initCmdArgs(String input) {
		HashMap<String, String> argsMap = super.initCmdArgs(input);
		argsMap.put("--backend", "yices");
		
//		argsMap.put("--verbosity", "4");
//		argsMap.put("--showphase", "parse");

		System.out.print(input + "\tyices");
		return argsMap;
	}
}
