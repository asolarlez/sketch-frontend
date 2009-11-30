package sketch.compiler.smt.z3;

import java.io.IOException;
import java.util.HashMap;

import junit.framework.Assert;


public class SketchRegressionTests extends sketch.compiler.smt.tests.SketchRegressionBlastBV {
    
	protected HashMap<String, String> initCmdArgs(String input) {
		HashMap<String, String> argsMap = super.initCmdArgs(input);
		argsMap.put("--backend", "z3");
		
		argsMap.put("--verbosity", "4");
		argsMap.put("--showphase", "lowering");
		System.out.print(input.substring(input.lastIndexOf("/")) + "\tz3");
		return argsMap;
	}
	
	@Override
	public void miniTest93() throws IOException, InterruptedException {
		Assert.fail("Uninterpreted function");
		super.miniTest93();
	}
	
	@Override
	public void miniTestb101() throws IOException, InterruptedException {
		Assert.fail("Uninterpreted function");
		super.miniTestb101();
	}
	
	@Override
	public void miniTestb147() throws IOException, InterruptedException {
//		Assert.fail("divide by zero guarded by a condition");
		super.miniTestb147();
	}
}
