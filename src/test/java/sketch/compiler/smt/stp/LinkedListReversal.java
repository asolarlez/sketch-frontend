package sketch.compiler.smt.stp;

import java.util.HashMap;

import sketch.compiler.smt.TestHarness;

public class LinkedListReversal extends sketch.compiler.smt.tests.LinkedListReversal {

	protected HashMap<String, String> initCmdArgs(String input) {
		HashMap<String, String> argsMap = super.initCmdArgs(input);
		argsMap.put("--backend", "stp");
		
		// argsMap.put("--verbosity", "4");
		// argsMap.put("--showphase", "parse");
		
		System.out.print(input + "\tstp");
		return argsMap;
	}

}
