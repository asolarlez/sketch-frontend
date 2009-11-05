package sketch.compiler.smt.stp;

import java.io.IOException;
import java.util.HashMap;

import org.junit.Assert;

public class StenRegressionTests extends
		sketch.compiler.smt.tests.StenRegressionTests {

	@Override
	protected HashMap<String, String> initCmdArgs(String input) {
		HashMap<String, String> argsMap = super.initCmdArgs(input);
		argsMap.put("--backend", "stp");
		
		 argsMap.put("--verbosity", "4");
		 argsMap.put("--showphase", "lowering");
		System.out.print(input + "\tstp");
		return argsMap;
	}
	
	@Override
	public void miniTest2() throws IOException, InterruptedException {
		Assert.fail("takes a long time to fail");
		super.miniTest2();
	}
	
	@Override
	public void miniTest25() throws IOException, InterruptedException {
		Assert.fail("takes a lot of memory, not finished");
		super.miniTest25();
	}
	
}
