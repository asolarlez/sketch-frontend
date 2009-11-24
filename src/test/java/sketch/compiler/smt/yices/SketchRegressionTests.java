package sketch.compiler.smt.yices;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashMap;

import junit.framework.Assert;

import org.junit.Test;

import sketch.compiler.smt.HoleSorter;

public class SketchRegressionTests extends sketch.compiler.smt.tests.SketchRegressionTests {

	protected HashMap<String, String> initCmdArgs(String input) {
		HashMap<String, String> argsMap = super.initCmdArgs(input);
		argsMap.put("--backend", "yices");
		
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
        // yices skips the value for H__0_0
        // thus can not use the default test
        runOneTest(toArgArray(argsMap));
        
        HoleSorter sorter = new HoleSorter(oracle);
        assertTrue(sorter.getHoleValueByOrder(0) == 1);
        assertTrue(sorter.getHoleValueByOrder(1) == 2);
        assertTrue(sorter.getHoleValueByOrder(2) == 2);
        assertTrue(sorter.getHoleValueByOrder(3) == 2);
        assertTrue(sorter.getHoleValueByOrder(4) == 2);
        assertTrue(sorter.getHoleValueByOrder(5) == 2);
        assertTrue(sorter.getHoleValueByOrder(6) == 2);
    }

}
