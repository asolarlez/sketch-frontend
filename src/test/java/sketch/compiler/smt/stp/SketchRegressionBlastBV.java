package sketch.compiler.smt.stp;

import java.io.IOException;
import java.util.HashMap;

import org.junit.Assert;
import org.junit.Test;

public class SketchRegressionBlastBV extends
		sketch.compiler.smt.tests.SketchRegressionBlastBV {

	protected HashMap<String, String> initCmdArgs(String input) {
		HashMap<String, String> argsMap = super.initCmdArgs(input);
		argsMap.put("--backend", "stp");
		
		 argsMap.put("--verbosity", "0");
//		 argsMap.put("--showphase", "lowering");
		 System.out.print(input.substring(input.lastIndexOf("/")) + 
		         "\tstp-blastbv");
		return argsMap;
	}
	
	// the following are failed tests
	
	@Override
	public void miniTest88() throws IOException, InterruptedException {
		Assert.fail("BigInteger needed");
		super.miniTest88();
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
	
	@Test
    public void miniTest50() throws IOException, InterruptedException {
	    Assert.fail("division by zero. internal to STP");
        super.miniTest50();
        
    }
	
	@Override
	public void miniTest51() throws IOException, InterruptedException {
       HashMap<String, String> argsMap = initCmdArgsWithFileName("miniTest51.sk");
       runOneTest(toArgArray(argsMap));
	}

		
}
