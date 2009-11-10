package sketch.compiler.seq;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashMap;

import org.junit.Assert;
import org.junit.Test;

import sketch.compiler.smt.HoleSorter;
import sketch.compiler.smt.TestHarness;

public abstract class SketchRegressionTests extends TestHarness{
	
	protected abstract HashMap<String, String> initCmdArgs(String string);
	protected abstract void runOneTest(String[] args) throws IOException, InterruptedException;
	
	@Test
	public void miniTest1() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest1.sk");

		runOneTest(toArgArray(argsMap));
	}

	

	@Test
	public void miniTest2() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest2.sk");

		runOneTest(toArgArray(argsMap));
	}

	@Test
	public void miniTest3() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest3.sk");

		runOneTest(toArgArray(argsMap));
	}

	@Test
	public void miniTest4() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest4.sk");

		runOneTest(toArgArray(argsMap));

	}

	@Test
	public void miniTest5() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest5.sk");

		runOneTest(toArgArray(argsMap));
	}

	@Test
	public void miniTest6() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest6.sk");

		runOneTest(toArgArray(argsMap));

	}
	
	@Test
	public void miniTest8() throws IOException, InterruptedException {
		
		HashMap<String, String> argsMap = initCmdArgs("miniTest8.sk");
		runOneTest(toArgArray(argsMap));
		

	}

	@Test
	public void miniTest10() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest10.sk");

		runOneTest(toArgArray(argsMap));
	}	

	@Test
	public void miniTest11() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest11.sk");

		runOneTest(toArgArray(argsMap));
	}

	@Test
	public void miniTest12() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest12.sk");

		runOneTest(toArgArray(argsMap));
	}

	@Test
	public void miniTest13() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest13.sk");

		runOneTest(toArgArray(argsMap));
	}

	@Test
	public void miniTest14() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest14.sk");

		runOneTest(toArgArray(argsMap));
	}

	@Test
	public void miniTest15() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest15.sk");
		
		runOneTest(toArgArray(argsMap));
	}

	@Test
	public void miniTest16() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest16.sk");

		runOneTest(toArgArray(argsMap));
	}

	@Test
	public void miniTest17() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest17.sk");

		runOneTest(toArgArray(argsMap));
	}

	@Test
	public void miniTest18() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest18.sk");

		runOneTest(toArgArray(argsMap));
	}

	@Test
	public void miniTest19() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest19.sk");

		runOneTest(toArgArray(argsMap));

	}

	@Test
	public void miniTest20() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest20.sk");

		runOneTest(toArgArray(argsMap));

	}
	
	@Test
	public void miniTest21() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest21.sk");

		runOneTest(toArgArray(argsMap));
	}

	@Test
	public void miniTest22() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest22.sk");

		runOneTest(toArgArray(argsMap));

	}

	@Test
	public void miniTest23() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest23.sk");

		runOneTest(toArgArray(argsMap));
	}

	@Test
	public void miniTest24() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest24.sk");

		runOneTest(toArgArray(argsMap));
	}

	@Test
	public void miniTest25() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest25.sk");

		runOneTest(toArgArray(argsMap));

		
		
	}

	@Test
	public void miniTest26() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest26.sk");
	
		runOneTest(toArgArray(argsMap));
	}
	
	@Test
	public void miniTest28() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest28.sk");
	
		runOneTest(toArgArray(argsMap));
	}
	
	@Test
	public void miniTest29() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest29.sk");
	
		runOneTest(toArgArray(argsMap));
	}

	@Test
	public void miniTest30() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest30.sk");

		runOneTest(toArgArray(argsMap));
	}

	@Test
	public void miniTest31() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest31.sk");

		runOneTest(toArgArray(argsMap));
	}

	@Test
	public void miniTest32() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest32.sk");

		runOneTest(toArgArray(argsMap));	
	}

	@Test
	public void miniTest33() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest33.sk");

		runOneTest(toArgArray(argsMap));
	}

	@Test
	public void miniTest34() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest34.sk");

		runOneTest(toArgArray(argsMap));
	}

	@Test
	public void miniTest35() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest35.sk");

		runOneTest(toArgArray(argsMap));
	}

	@Test
	public void miniTest36() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest36.sk");

		runOneTest(toArgArray(argsMap));
	}

	@Test
	public void miniTest37() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest37.sk");

		runOneTest(toArgArray(argsMap));
	}
	
	@Test
	public void miniTest38() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest38.sk");

		runOneTest(toArgArray(argsMap));
	}

	@Test
	public void miniTest39() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest39.sk");

		runOneTest(toArgArray(argsMap));
	}

	@Test
	public void miniTest40() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest40.sk");

		runOneTest(toArgArray(argsMap));
	}

	@Test
	public void miniTest41() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest41.sk");

		runOneTest(toArgArray(argsMap));
	}
	
	@Test
	public void miniTest42() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest42.sk");

		runOneTest(toArgArray(argsMap));
	}

	@Test
	public void miniTest43() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest43.sk");

		runOneTest(toArgArray(argsMap));
		
	}

	@Test
	public void miniTest45() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest45.sk");

		runOneTest(toArgArray(argsMap));
		
	}

	@Test
	public void miniTest46() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest46.sk");

		runOneTest(toArgArray(argsMap));
		
	}

	@Test
	public void miniTest47() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest47.sk");

		runOneTest(toArgArray(argsMap));
		
	}

	@Test
	public void miniTest48() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest48.sk");

		runOneTest(toArgArray(argsMap));
	}
	
	@Test
	public void miniTest49() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest49.sk");
		runOneTest(toArgArray(argsMap)); 
	}
	
	@Test
	public void miniTest50() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest50.sk");
		runOneTest(toArgArray(argsMap));
		
		// mod 
	}

	@Test
	public void miniTest51() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest51.sk");

		runOneTest(toArgArray(argsMap));
	}

	@Test
	public void miniTest52() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest52.sk");
		runOneTest(toArgArray(argsMap));
		
		// mod
	}
	
	@Test
	public void miniTest53() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest53.sk");

		runOneTest(toArgArray(argsMap));

	}

	 @Test
	 public void miniTest54() throws IOException, InterruptedException {
		 HashMap<String, String> argsMap = initCmdArgs("miniTest54.sk");
			
		 runOneTest(toArgArray(argsMap));
		 // bit array too big to be represented with int 32
	 }

	@Test
	public void miniTest55() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest55.sk");

		runOneTest(toArgArray(argsMap));
	}

	@Test
	public void miniTest56() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest56.sk");

		runOneTest(toArgArray(argsMap));
	}

	@Test
	public void miniTest57() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest57.sk");

		runOneTest(toArgArray(argsMap));
	}

	@Test
	public void miniTest60() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest60.sk");

		runOneTest(toArgArray(argsMap));
		
	}
	
	@Test
	public void miniTest61() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest61.sk");

		runOneTest(toArgArray(argsMap));
	}

	@Test
	public void miniTest62() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest62.sk");

		runOneTest(toArgArray(argsMap));
	}

	@Test
	public void miniTest63() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest63.sk");

		runOneTest(toArgArray(argsMap));
	}

	@Test
	public void miniTest64() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest64.sk");

		runOneTest(toArgArray(argsMap));	
	}

	@Test
	public void miniTest65() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest65.sk");

		runOneTest(toArgArray(argsMap));

	}

	@Test
	public void miniTest66() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest66.sk");

		runOneTest(toArgArray(argsMap));
	}

	@Test
	public void miniTest67() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest67.sk");

		runOneTest(toArgArray(argsMap));
	}
	
	@Test
	public void miniTest68() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest68.sk");
		runOneTest(toArgArray(argsMap));
		
		
	}

	@Test
	public void miniTest69() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest69.sk");

		runOneTest(toArgArray(argsMap));

		
	}

	@Test
	public void miniTest70() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest70.sk");

		runOneTest(toArgArray(argsMap));
		
	}

	@Test
	public void miniTest71() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest71.sk");

		runOneTest(toArgArray(argsMap));
		
	}

	@Test
	public void miniTest72() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest72.sk");

		runOneTest(toArgArray(argsMap));
		
	}

	@Test
	public void miniTest73() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest73.sk");

		runOneTest(toArgArray(argsMap));
		
	}

	@Test
	public void miniTest74() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest74.sk");

		runOneTest(toArgArray(argsMap));

	}

	@Test
	public void miniTest75() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest75.sk");

		runOneTest(toArgArray(argsMap));
		
	}

	@Test
	public void miniTest76() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest76.sk");

		runOneTest(toArgArray(argsMap));
		
	}

	@Test
	public void miniTest77() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest77.sk");

		runOneTest(toArgArray(argsMap));
		
	}

	@Test
	public void miniTest78() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest78.sk");

		runOneTest(toArgArray(argsMap));
	}

	@Test
	public void miniTest79() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest79.sk");

		runOneTest(toArgArray(argsMap));
	}

	@Test
	public void miniTest80() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest80.sk");

		runOneTest(toArgArray(argsMap));
		
	}

	@Test
	public void miniTest81() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest81.sk");

		runOneTest(toArgArray(argsMap));
	}

	@Test
	public void miniTest82() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest82.sk");

		runOneTest(toArgArray(argsMap));

	}

	@Test
	public void miniTest83() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest83.sk");

		runOneTest(toArgArray(argsMap));

	}

	@Test
	public void miniTest84() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest84.sk");

		runOneTest(toArgArray(argsMap));

	}

	 @Test
	 public void miniTest85() throws IOException, InterruptedException {
		 HashMap<String, String> argsMap = initCmdArgs("miniTest85.sk");
		
		 runOneTest(toArgArray(argsMap));
				
		 
		 // assertTrue((sorter.getHoleValueByOrder(0) == 2));
		 // assertTrue((sorter.getHoleValueByOrder(1) != 0));
		 // assertTrue((sorter.getHoleValueByOrder(2) != 0));
		 // infinite CEGIS loop, due to yices not showing full model, H__1 is not show in the output
	 }

	@Test
	public void miniTest86() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest86.sk");

		runOneTest(toArgArray(argsMap));

	}

	@Test
	public void miniTest87() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest87.sk");

		runOneTest(toArgArray(argsMap));
	}
	
	@Test
	public void miniTest88() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest88.sk");
		runOneTest(toArgArray(argsMap));
		
		// mod
	}

	@Test
	public void miniTest89() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest89.sk");

		runOneTest(toArgArray(argsMap));

	}

	@Test
	public void miniTest90() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest90.sk");

		runOneTest(toArgArray(argsMap));
	}

	@Test
	public void miniTest91() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest91.sk");

		runOneTest(toArgArray(argsMap));

	}

	 @Test
	 public void miniTest92() throws IOException, InterruptedException {
		 HashMap<String, String> argsMap = initCmdArgs("miniTest92.sk");

		 runOneTest(toArgArray(argsMap));
			
		 // failed because BV of size > 32 can not be represented with int. Need
		 // to use BigInteger to track them.
	 }
	
	@Test
	public void miniTest93() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest93.sk");
		runOneTest(toArgArray(argsMap));
		
	}

	@Test
	public void miniTest94() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest94.sk");

		runOneTest(toArgArray(argsMap));
	}

	@Test
	public void miniTest95() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest95.sk");

		runOneTest(toArgArray(argsMap));

	}

	@Test
	public void miniTest96() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest96.sk");

		runOneTest(toArgArray(argsMap));

	}

	@Test
	public void miniTest97() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest97.sk");

		runOneTest(toArgArray(argsMap));
	}

	@Test
	public void miniTest98() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest98.sk");

		runOneTest(toArgArray(argsMap));

	}

	@Test
	public void miniTest99() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTest99.sk");

		runOneTest(toArgArray(argsMap));

	}

	@Test
	public void miniTestb100() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTestb100.sk");

		runOneTest(toArgArray(argsMap));
	}
	
	@Test
	public void miniTestb101() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTestb101.sk");
		runOneTest(toArgArray(argsMap));	
	}

	@Test
	public void miniTestb102() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTestb102.sk");

		runOneTest(toArgArray(argsMap));

		// big test
		// hard to verify
	}

	@Test
	public void miniTestb103() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTestb103.sk");

		runOneTest(toArgArray(argsMap));

		// big test
		// hard to verify
	}

	@Test
	public void miniTestb104() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTestb104.sk");

		runOneTest(toArgArray(argsMap));

		// big test
		// hard to verify
	}
	
	@Test
	public void miniTestb105() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTestb105.sk");
		runOneTest(toArgArray(argsMap));
	}
	
	@Test
	public void miniTestb106() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTestb106.sk");
		runOneTest(toArgArray(argsMap));
	}

	@Test
	public void miniTestb107() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTestb107.sk");

		runOneTest(toArgArray(argsMap));

	}

	@Test
	public void miniTestb108() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTestb108.sk");

		runOneTest(toArgArray(argsMap));
	}

	@Test
	public void miniTestb109() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTestb109.sk");

		runOneTest(toArgArray(argsMap));
	}

	@Test
	public void miniTestb110() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTestb110.sk");

		runOneTest(toArgArray(argsMap));
	}

	@Test
	public void miniTestb111() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTestb111.sk");

		runOneTest(toArgArray(argsMap));

	}

	@Test
	public void miniTestb112() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTestb112.sk");

		runOneTest(toArgArray(argsMap));

	}

	@Test
	public void miniTestb113() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTestb113.sk");

		runOneTest(toArgArray(argsMap));

	}

	@Test
	public void miniTestb114() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTestb114.sk");

		runOneTest(toArgArray(argsMap));
	}

	@Test
	public void miniTestb116() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTestb116.sk");

		runOneTest(toArgArray(argsMap));

	}

	@Test
	public void miniTestb117() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTestb117.sk");

		runOneTest(toArgArray(argsMap));

	}

	@Test
	public void miniTestb118() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTestb118.sk");

		runOneTest(toArgArray(argsMap));
	}
	
	@Test
	public void miniTestb120() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTestb120.sk");
		runOneTest(toArgArray(argsMap));
	}

	@Test
	public void miniTestb121() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTestb121.sk");

		runOneTest(toArgArray(argsMap));
	}

	@Test
	public void miniTestb122() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTestb122.sk");

		runOneTest(toArgArray(argsMap));

	}

	@Test
	public void miniTestb123() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTestb123.sk");

		runOneTest(toArgArray(argsMap));

	}

	@Test
	public void miniTestb124() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTestb124.sk");

		runOneTest(toArgArray(argsMap));

	}

	@Test
	public void miniTestb125() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTestb125.sk");

		runOneTest(toArgArray(argsMap));

	}

	@Test
	public void miniTestb126() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTestb126.sk");

		runOneTest(toArgArray(argsMap));

	}

	@Test
	public void miniTestb127() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTestb127.sk");

		runOneTest(toArgArray(argsMap));

	}

	@Test
	public void miniTestb128() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTestb128.sk");

		runOneTest(toArgArray(argsMap));

	}

	@Test
	public void miniTestb129() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTestb129.sk");

		runOneTest(toArgArray(argsMap));

	}

	@Test
	public void miniTestb130() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTestb130.sk");

		runOneTest(toArgArray(argsMap));

	}

	@Test
	public void miniTestb131() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTestb131.sk");

		runOneTest(toArgArray(argsMap));

	}

	@Test
	public void miniTestb132() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTestb132.sk");

		runOneTest(toArgArray(argsMap));

	}

	@Test
	public void miniTestb133() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTestb133.sk");

		runOneTest(toArgArray(argsMap));

	}

	@Test
	public void miniTestb134() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTestb134.sk");

		runOneTest(toArgArray(argsMap));

	}

	@Test
	public void miniTestb135() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTestb135.sk");

		runOneTest(toArgArray(argsMap));

	}

	@Test
	public void miniTestb136() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTestb136.sk");

		runOneTest(toArgArray(argsMap));
	}

	@Test
	public void miniTestb137() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTestb137.sk");

		runOneTest(toArgArray(argsMap));

	}

	@Test
	public void miniTestb138() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTestb138.sk");

		runOneTest(toArgArray(argsMap));

	}

	@Test
	public void miniTestb139() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTestb139.sk");

		runOneTest(toArgArray(argsMap));

	}

	@Test
	public void miniTestb140() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTestb140.sk");

		runOneTest(toArgArray(argsMap));
	}

	@Test
	public void miniTestb141() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTestb141.sk");

		runOneTest(toArgArray(argsMap));

		// an infinite loop after reducing the num bits for int to 8

	}

	@Test
	public void miniTestb142() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTestb142.sk");

		runOneTest(toArgArray(argsMap));

	}

	@Test
	public void miniTestb143() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTestb143.sk");

		runOneTest(toArgArray(argsMap));

	}

	@Test
	public void miniTestb144() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTestb144.sk");

		runOneTest(toArgArray(argsMap));

		// reorder block
		// too hard to verify in detail
	}

	@Test
	public void miniTestb145() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTestb145.sk");

		runOneTest(toArgArray(argsMap));

	}

	@Test
	public void miniTestb146() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTestb146.sk");

		runOneTest(toArgArray(argsMap));

	}

	@Test
	public void miniTestb147() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTestb147.sk");

		runOneTest(toArgArray(argsMap));
	}

	@Test
	public void miniTestb148() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTestb148.sk");

		runOneTest(toArgArray(argsMap));
	}

	@Test
	public void miniTestb149() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTestb149.sk");

		runOneTest(toArgArray(argsMap));
	}

	@Test
	public void miniTestb150() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTestb150.sk");

		runOneTest(toArgArray(argsMap));
	}

	@Test
	public void miniTestb151() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTestb151.sk");

		runOneTest(toArgArray(argsMap));
	}

	@Test
	public void miniTestb152() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTestb152.sk");

		runOneTest(toArgArray(argsMap));
	}

	@Test
	public void miniTestb153() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTestb153.sk");

		runOneTest(toArgArray(argsMap));

		// reorder block
		// too hard to verify in detail
	}

	@Test
	public void miniTestb154() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTestb154.sk");

		runOneTest(toArgArray(argsMap));
	}

	@Test
	public void miniTestb155() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTestb155.sk");

		runOneTest(toArgArray(argsMap));
	}

	@Test
	public void miniTestb156() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTestb156.sk");

		runOneTest(toArgArray(argsMap));
	}

	@Test
	public void miniTestb157() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTestb157.sk");

		runOneTest(toArgArray(argsMap));
	}

	@Test
	public void miniTestb158() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTestb158.sk");

		runOneTest(toArgArray(argsMap));

	}

	@Test
	public void miniTestb159() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTestb159.sk");

		runOneTest(toArgArray(argsMap));

	}

	@Test
	public void miniTestb160() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTestb160.sk");
		runOneTest(toArgArray(argsMap));
		
		// mod
	}
	@Test
	public void miniTestb161() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTestb161.sk");
		runOneTest(toArgArray(argsMap));
		
		// mod
	}
	
	@Test
	public void miniTestb162() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTestb162.sk");

		runOneTest(toArgArray(argsMap));

	}

	@Test
	public void miniTestb163() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTestb163.sk");

		runOneTest(toArgArray(argsMap));

	}

	@Test
	public void miniTestb164() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTestb164.sk");

		runOneTest(toArgArray(argsMap));
		// too complicated to express
	}

	@Test
	public void miniTestb165() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTestb165.sk");

		runOneTest(toArgArray(argsMap));

	}

	@Test
	public void miniTestb166() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTestb166.sk");

		runOneTest(toArgArray(argsMap));
	}

	@Test
	public void miniTestb167() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTestb167.sk");

		runOneTest(toArgArray(argsMap));
	}

	@Test
	public void miniTestb168() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTestb168.sk");

		runOneTest(toArgArray(argsMap));

	}

	@Test
	public void miniTestb169() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTestb169.sk");

		runOneTest(toArgArray(argsMap));

	}

	@Test
	public void miniTestb170() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTestb170.sk");

		runOneTest(toArgArray(argsMap));
	}

	@Test
	public void miniTestb171() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTestb171.sk");

		runOneTest(toArgArray(argsMap));
	}

	@Test
	public void miniTestb172() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTestb172.sk");

		runOneTest(toArgArray(argsMap));
	}

	@Test
	public void miniTestb173() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTestb173.sk");

		runOneTest(toArgArray(argsMap));

	}

	@Test
	public void miniTestb174() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTestb174.sk");

		runOneTest(toArgArray(argsMap));
	}

	@Test
	public void miniTestb175() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTestb175.sk");

		runOneTest(toArgArray(argsMap));

	}

	@Test
	public void miniTestb176() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTestb176.sk");

		runOneTest(toArgArray(argsMap));
	}

	@Test
	public void miniTestb177() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTestb177.sk");
		runOneTest(toArgArray(argsMap));

	}

	@Test
	public void miniTestb178() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTestb178.sk");
		runOneTest(toArgArray(argsMap));

	}

	@Test
	public void miniTestb179() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("miniTestb179.sk");
		runOneTest(toArgArray(argsMap));
	}

}
