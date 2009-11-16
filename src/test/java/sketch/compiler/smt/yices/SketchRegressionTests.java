package sketch.compiler.smt.yices;

import java.io.IOException;
import java.util.HashMap;

import junit.framework.Assert;

import org.junit.Test;

public class SketchRegressionTests extends sketch.compiler.smt.tests.SketchRegressionTests {

	protected HashMap<String, String> initCmdArgs(String input) {
		HashMap<String, String> argsMap = super.initCmdArgs(input);
		argsMap.put("--backend", "yices2");
		
//		 argsMap.put("--verbosity", "4");
		// argsMap.put("--showphase", "parse");
		System.out.print(input.substring(input.lastIndexOf("/")) + "\tyices");
		return argsMap;
	}

	
	@Override
	public void miniTest49() throws IOException, InterruptedException {
		Assert.fail("mod");
		super.miniTest49();
	}
	
	@Override
	public void miniTest50() throws IOException, InterruptedException {
		Assert.fail("mod");
		super.miniTest50();
	}
	
	@Override
	public void miniTest52() throws IOException, InterruptedException {
		Assert.fail("mod");
		super.miniTest52();
	}
	
	
	@Override
	public void miniTest88() throws IOException, InterruptedException {
		Assert.fail("mod");
		super.miniTest88();
	}
	
	@Override
	public void miniTest92() throws IOException, InterruptedException {
		Assert.fail("need to use BigInteger");
		super.miniTest92();
	}
	
	@Override
    public void miniTest93() throws IOException, InterruptedException {
        Assert.fail("uninterpreted function");
        super.miniTest93();
    }
	
	@Override
	public void miniTestb101() throws IOException, InterruptedException {
		Assert.fail("uninterpreted function");
	}
	
	@Override
	public void miniTestb160() throws IOException, InterruptedException {
		Assert.fail("mod");
		super.miniTestb160();
	}
	
	@Override
	public void miniTestb161() throws IOException, InterruptedException {
		Assert.fail("mod");
		super.miniTestb161();
	}
	
	@Test
    public void miniTest51() throws IOException, InterruptedException {
        HashMap<String, String> argsMap = initCmdArgsWithFileName("miniTest51.sk");

        runOneTest(toArgArray(argsMap));
    }

}
