package sketch.compiler.smt.yices;

import java.io.IOException;
import java.util.HashMap;

import junit.framework.Assert;

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
	public void miniTest8() throws IOException, InterruptedException {
		Assert.fail("verification takes a long time. tried running it over 1.5 hours. still not finished.");
//		super.miniTest8();
	}
	
	@Override
	public void miniTest15() throws IOException, InterruptedException {
		Assert.fail("yices not showing full model");
		super.miniTest15();
		
		
	}
	
	@Override
	public void miniTest26() throws IOException, InterruptedException {
		Assert.fail("yices not showing full model");
		super.miniTest26();
	}
	
	@Override
	public void miniTest28() throws IOException, InterruptedException {
		Assert.fail("yices not showing full model");
		super.miniTest28();
	}
	
	@Override
	public void miniTest29() throws IOException, InterruptedException {
		Assert.fail("yices not showing full model");
		super.miniTest29();
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
	public void miniTest54() throws IOException, InterruptedException {
		Assert.fail("BigInteger required");
		super.miniTest54();
	}
	
	@Override
	public void miniTest85() throws IOException, InterruptedException {
		Assert.fail("yices not showing full model");
		super.miniTest85();
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
	public void miniTestb101() throws IOException, InterruptedException {
		Assert.fail("uninterpreted function");
//		super.miniTestb101();
	}
	
	@Override
	public void miniTestb120() throws IOException, InterruptedException {
		Assert.fail("infinite loop. verifier is not finding good counterexample");
//		super.miniTestb120();
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
	
	@Override
	public void miniTest93() throws IOException, InterruptedException {
		Assert.fail("uninterpreted function");
		super.miniTest93();
	}
	
	@Override
	public void miniTestb105() throws IOException, InterruptedException {
		Assert.fail("infinite CEGIS loop b/c of adverse sketch");
		super.miniTestb105();
	}
	
	@Override
	public void miniTestb106() throws IOException, InterruptedException {
		Assert.fail("infinite CEGIS loop b/c of adverse sketch");
		super.miniTestb106();
	}
}
