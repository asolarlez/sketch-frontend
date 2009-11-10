package sketch.compiler.smt.yices;

import java.io.IOException;
import java.util.HashMap;

import junit.framework.Assert;

public class StenRegressionTests extends sketch.compiler.smt.tests.StenRegressionTests {
	
	protected HashMap<String, String> initCmdArgs(String input) {
		HashMap<String, String> argsMap = super.initCmdArgs(input);
		argsMap.put("--backend", "yices");
		
		// argsMap.put("--verbosity", "4");
		// argsMap.put("--showphase", "parse");
		System.out.print(input + "\tyices");
		return argsMap;
	}
	
	@Override
	public void miniTest27() throws IOException, InterruptedException {
		Assert.fail("synthesizer failed to solve");
		super.miniTest27();
	}
	
}
