package sketch.compiler.smt.yices;

import static org.junit.Assert.assertTrue;

import java.util.HashMap;

import org.junit.Test;

import sketch.compiler.main.seq.SequentialSMTSketchMain;

public class LanguageBasicBlastBV extends sketch.compiler.smt.tests.LanguageBasicBlastBV  {

	protected HashMap<String, String> initCmdArgs(String input) {
		HashMap<String, String> argsMap = super.initCmdArgs(input);
		argsMap.put("--backend", "yices");
		
		 argsMap.put("--verbosity", "4");
		 argsMap.put("--showphase", "lowering");
		System.out.print(input + "\tyices");
		return argsMap;
	}
	
	@Override
	public void testIntegers() throws Exception {
		super.testIntegers();
//		 yices not showing solution for the holes
		
	}
	
}
