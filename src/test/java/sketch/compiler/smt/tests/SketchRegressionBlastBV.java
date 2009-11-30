package sketch.compiler.smt.tests;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashMap;

import org.junit.After;
import org.junit.Test;

import sketch.compiler.main.seq.SequentialSMTSketchMain;
import sketch.compiler.smt.HoleSorter;
import sketch.compiler.smt.partialeval.SmtValueOracle;

public abstract class SketchRegressionBlastBV extends sketch.compiler.seq.SketchRegressionTests {
    
    @Override
    protected HashMap<String, String> initCmdArgsWithFileName(String input) {
        return initCmdArgs("src/test/sk/seq/" + input);
    }
	protected HashMap<String, String> initCmdArgs(String inputPath) {
	    
		HashMap<String, String> argsMap = new HashMap<String, String>();

		argsMap.put("--smtpath", System.getenv("smtpath"));
		
		argsMap.put("--bv", null);
		argsMap.put("--uselet", null);
		argsMap.put("--arrayOOBPolicy", "assertions");

		argsMap.put("--heapsize", "10");
		argsMap.put("--intbits", "5");
		
		argsMap.put("--outputdir", "output/");
		argsMap.put("--tmpdir", tmpDirStr);
		argsMap.put("--keeptmpfiles", null);

		
		// argsMap.put("--trace", null);
		argsMap.put(inputPath, null);

		return argsMap;
	}

	public void runOneTest(String[] args) throws IOException, InterruptedException {
		oracle = null;
		stat = null;
		SequentialSMTSketchMain main = new SequentialSMTSketchMain(args);
		assertTrue(main.runBeforeGenerateCode());
		oracle = (SmtValueOracle) main.getOracle();
		stat = main.getSolutionStat();
	}
	
	@After
	public void printTiming() {
		if (stat == null)
			System.out.println("\tFAILED");
		else
			System.out.println("\t" + stat.getSolutionTimeMs() + "\t" + stat.getIterations());
	}
	
	@Test
	public void miniTest1() throws IOException, InterruptedException {
		super.miniTest1();
		
		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue((sorter.getHoleValueByOrder(0) != 0)
				|| (sorter.getHoleValueByOrder(0) == 0 && sorter
						.getHoleValueByOrder(1) != 0));
	}

	@Test
	public void miniTest2() throws IOException, InterruptedException {
		super.miniTest2();

		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue((sorter.getHoleValueByOrder(0) == 1));
	}

	@Test
	public void miniTest3() throws IOException, InterruptedException {
		super.miniTest3();
		assertTrue("No hole, iteration # should be 0", stat.getIterations() == 0);
	}

	@Test
	public void miniTest4() throws IOException, InterruptedException {
		super.miniTest4();
		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue((sorter.getHoleValueByOrder(0) == 2));
	}

	@Test
	public void miniTest5() throws IOException, InterruptedException {
		super.miniTest5();
		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue((sorter.getHoleValueByOrder(0) == 2)
				&& (sorter.getHoleValueByOrder(1) == 3)
				|| ((sorter.getHoleValueByOrder(0) == 3) && (sorter
						.getHoleValueByOrder(1) == 2)));
	}

	@Test
	public void miniTest6() throws IOException, InterruptedException {
		super.miniTest6();
		HoleSorter sorter = new HoleSorter(oracle);

		assertTrue((sorter.getHoleValueByOrder(0) == 2));
	}
	
	@Test
	public void miniTest8() throws IOException, InterruptedException {
		super.miniTest8();
		
		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue(sorter.getHoleValueByOrder(0) == 3);	
	}

	@Test
	public void miniTest10() throws IOException, InterruptedException {
		super.miniTest10();
		
		HoleSorter sorter = new HoleSorter(oracle);

	}	

	@Test
	public void miniTest11() throws IOException, InterruptedException {
		super.miniTest11();

		assertTrue("No hole, iteration # should be 0", stat.getIterations() == 0);
	}

	@Test
	public void miniTest12() throws IOException, InterruptedException {
		super.miniTest12();

		assertTrue("No hole, iteration # should be 0", stat.getIterations() == 0);
	}

	@Test
	public void miniTest13() throws IOException, InterruptedException {
		super.miniTest13();
		
		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue((sorter.getHoleValueByOrder(0) == 2));
		assertTrue((sorter.getHoleValueByOrder(1) == 2));
	}

	@Test
	public void miniTest14() throws IOException, InterruptedException {
		super.miniTest14();

		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue((sorter.getHoleValueByOrder(0) == 3));
	}

	@Test
	public void miniTest15() throws IOException, InterruptedException {
		super.miniTest15();
		
		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue(sorter.getHoleValueByOrder(0) != 0);
		assertTrue(sorter.getHoleValueByOrder(1) != 0);
	
	}

	@Test
	public void miniTest16() throws IOException, InterruptedException {
		super.miniTest16();
		
		assertTrue("No hole, iteration # should be 0", stat.getIterations() == 0);
	}

	@Test
	public void miniTest17() throws IOException, InterruptedException {
		super.miniTest17();

		
		assertTrue("No hole, iteration # should be 0", stat.getIterations() == 0);
	}

	@Test
	public void miniTest18() throws IOException, InterruptedException {
		super.miniTest18();

		
	}

	@Test
	public void miniTest19() throws IOException, InterruptedException {
		super.miniTest19();

		
		assertTrue("No hole, iteration # should be 0", stat.getIterations() == 0);
	}

	@Test
	public void miniTest20() throws IOException, InterruptedException {
		super.miniTest20();

		

		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue((sorter.getHoleValueByOrder(0) == 2));
	}
	
	@Test
	public void miniTest21() throws IOException, InterruptedException {
		super.miniTest21();

		
	}

	@Test
	public void miniTest22() throws IOException, InterruptedException {
		super.miniTest22();

		

		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue((sorter.getHoleValueByOrder(0) != 0));
		assertTrue((sorter.getHoleValueByOrder(1) == 2));
	}

	@Test
	public void miniTest23() throws IOException, InterruptedException {
		super.miniTest23();
		
	}

	@Test
	public void miniTest24() throws IOException, InterruptedException {
		super.miniTest24();

		

		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue((sorter.getHoleValueByOrder(0) > 2));

	}

	@Test
	public void miniTest25() throws IOException, InterruptedException {
		super.miniTest25();

		

		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue("No hole, iteration # should be 0", stat.getIterations() == 0);
	}

	@Test
	public void miniTest26() throws IOException, InterruptedException {
		super.miniTest26();
	
		
	
		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue((sorter.getHoleValueByOrder(0) != 0));
			
		// yices not showing full model
	}
	
	@Test
	public void miniTest28() throws IOException, InterruptedException {
		super.miniTest28();
	
		
			
		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue((sorter.getHoleValueByOrder(0) == 0));
		assertTrue((sorter.getHoleValueByOrder(1) != 0));
		// yices not showing full model
	}
	
	@Test
	public void miniTest29() throws IOException, InterruptedException {
		super.miniTest29();
	
		
	
		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue((sorter.getHoleValueByOrder(0) == 1));
	}

	@Test
	public void miniTest30() throws IOException, InterruptedException {
		super.miniTest30();

		

		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue((sorter.getHoleValueByOrder(0) == 2));
	}

	@Test
	public void miniTest31() throws IOException, InterruptedException {
		super.miniTest31();

		

		// very complicated to express the condition to check
		// resolved sketch:
		// if (in_0.sub<1>(0)) {
		// x_2 = (in_0.sub<1>(2) & bitvec<1>(bitvec<1>(1U))) |
		// bitvec<1>((unsigned)bitvec<1>(0U));
		// } else {
		// x_2 = bitvec<1>(1U);
		// }
		// s_1 = in_0.sub<1>(x_2);
	}

	@Test
	public void miniTest32() throws IOException, InterruptedException {
		super.miniTest32();
		HoleSorter sorter = new HoleSorter(oracle);
	}

	@Test
	public void miniTest33() throws IOException, InterruptedException {
		super.miniTest33();

		

		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue((sorter.getHoleValueByOrder(0) == 2));
	}

	@Test
	public void miniTest34() throws IOException, InterruptedException {
		super.miniTest34();

		

		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue((sorter.getHoleValueByOrder(0) == 2));
	}

	@Test
	public void miniTest35() throws IOException, InterruptedException {
		super.miniTest35();

		

		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue((sorter.getHoleValueByOrder(0) == 2));
		assertTrue((sorter.getHoleValueByOrder(1) == 2));
	}

	@Test
	public void miniTest36() throws IOException, InterruptedException {
		super.miniTest36();

		

		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue((sorter.getHoleValueByOrder(0) == 2));
		assertTrue((sorter.getHoleValueByOrder(1) == 2));
	}

	@Test
	public void miniTest37() throws IOException, InterruptedException {
		super.miniTest37();

		

		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue((sorter.getHoleValueByOrder(0) == sorter
				.getHoleValueByOrder(2)));
		assertTrue((sorter.getHoleValueByOrder(1) == 2));

	}
	
	@Test
	public void miniTest38() throws IOException, InterruptedException {
		super.miniTest38();

		
	}

	@Test
	public void miniTest39() throws IOException, InterruptedException {
		super.miniTest39();

		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue((sorter.getHoleValueByOrder(0) == 2));
		assertTrue((sorter.getHoleValueByOrder(1) == 2));
	}

	@Test
	public void miniTest40() throws IOException, InterruptedException {
		super.miniTest40();

		

		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue((sorter.getHoleValueByOrder(0) >= 2));
		assertTrue((sorter.getHoleValueByOrder(1) != 0));
	}

	@Test
	public void miniTest41() throws IOException, InterruptedException {
		super.miniTest41();

		

		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue(sorter.getHoleValueByOrder(0)
				* sorter.getHoleValueByOrder(1) == 4);
	}

	@Test
	public void miniTest42() throws IOException, InterruptedException {
		super.miniTest42();

		

		assertTrue("No hole, iteration # should be 0", stat.getIterations() == 0);
	}

	@Test
	public void miniTest43() throws IOException, InterruptedException {
		super.miniTest43();

		
		assertTrue("No hole, iteration # should be 0", stat.getIterations() == 0);
	}

	@Test
	public void miniTest45() throws IOException, InterruptedException {
		super.miniTest45();

		
		assertTrue("No hole, iteration # should be 0", stat.getIterations() == 0);
	}

	@Test
	public void miniTest46() throws IOException, InterruptedException {
		super.miniTest46();

		
		assertTrue("No hole, iteration # should be 0", stat.getIterations() == 0);
	}

	@Test
	public void miniTest47() throws IOException, InterruptedException {
		super.miniTest47();

		
		assertTrue("No hole, iteration # should be 0", stat.getIterations() == 0);
	}

	@Test
	public void miniTest48() throws IOException, InterruptedException {
		super.miniTest48();

		
	}
	
	@Test
	public void miniTest49() throws IOException, InterruptedException {
		super.miniTest49();
		 
	}
	
	@Test
	public void miniTest50() throws IOException, InterruptedException {
		super.miniTest50();
		
	}

	@Test
	public void miniTest51() throws IOException, InterruptedException {
		super.miniTest51();

		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue(sorter.getHoleValueByOrder(0) == 0);
		assertTrue(sorter.getHoleValueByOrder(1) == 1);
		assertTrue(sorter.getHoleValueByOrder(2) == 2);
		assertTrue(sorter.getHoleValueByOrder(3) == 2);
		assertTrue(sorter.getHoleValueByOrder(4) == 2);
		assertTrue(sorter.getHoleValueByOrder(5) == 2);
		assertTrue(sorter.getHoleValueByOrder(6) == 2);
		assertTrue(sorter.getHoleValueByOrder(7) == 2);
	}

	@Test
	public void miniTest52() throws IOException, InterruptedException {
		super.miniTest52();
	}
	
	@Test
	public void miniTest53() throws IOException, InterruptedException {
		super.miniTest53();

		

		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue(sorter.getHoleValueByOrder(0) == 3);
	}

	 @Test
	 public void miniTest54() throws IOException, InterruptedException {
		 super.miniTest54();
		 // bit array too big to be represented with int 32
	 }

	@Test
	public void miniTest55() throws IOException, InterruptedException {
		super.miniTest55();
	}

	@Test
	public void miniTest56() throws IOException, InterruptedException {
		super.miniTest56();
	}

	@Test
	public void miniTest57() throws IOException, InterruptedException {
		super.miniTest57();
	}

	@Test
	public void miniTest60() throws IOException, InterruptedException {
		super.miniTest60();
		assertTrue("No hole, iteration # should be 0", stat.getIterations() == 0);
	}
	
	@Test
	public void miniTest61() throws IOException, InterruptedException {
		super.miniTest61();
	}

	@Test
	public void miniTest62() throws IOException, InterruptedException {
		super.miniTest62();
//		Assert.fail("unverified. worth verifying the output");
	}

	@Test
	public void miniTest63() throws IOException, InterruptedException {
		super.miniTest63();
		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue((sorter.getHoleValueByOrder(0) == 1) ||
		        (sorter.getHoleValueByOrder(0) == 3));
	}

	@Test
	public void miniTest64() throws IOException, InterruptedException {
		super.miniTest64();
		// all holes are zero
		assertTrue(oracle.size() == 0);
	}

	@Test
	public void miniTest65() throws IOException, InterruptedException {
		super.miniTest65();
	}

	@Test
	public void miniTest66() throws IOException, InterruptedException {
		super.miniTest66();
		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue((sorter.getHoleValueByOrder(0) == 2));
	}

	@Test
	public void miniTest67() throws IOException, InterruptedException {
		super.miniTest67();
		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue((sorter.getHoleValueByOrder(0) == 2));
	}
	
	@Test
	public void miniTest68() throws IOException, InterruptedException {
		super.miniTest68();
		assertTrue("No hole, iteration # should be 0", stat.getIterations() == 0);
	}

	@Test
	public void miniTest69() throws IOException, InterruptedException {
		super.miniTest69();
		assertTrue("No hole, iteration # should be 0", stat.getIterations() == 0);
	}

	@Test
	public void miniTest70() throws IOException, InterruptedException {
		super.miniTest70();
		assertTrue("No hole, iteration # should be 0", stat.getIterations() == 0);
	}

	@Test
	public void miniTest71() throws IOException, InterruptedException {
		super.miniTest71();

		
		assertTrue("No hole, iteration # should be 0", stat.getIterations() == 0);
	}

	@Test
	public void miniTest72() throws IOException, InterruptedException {
		super.miniTest72();

		
		assertTrue("No hole, iteration # should be 0", stat.getIterations() == 0);
	}

	@Test
	public void miniTest73() throws IOException, InterruptedException {
		super.miniTest73();

		
		assertTrue("No hole, iteration # should be 0", stat.getIterations() == 0);
	}

	@Test
	public void miniTest74() throws IOException, InterruptedException {
		super.miniTest74();

		

	}

	@Test
	public void miniTest75() throws IOException, InterruptedException {
		super.miniTest75();

		
		assertTrue("No hole, iteration # should be 0", stat.getIterations() == 0);
	}

	@Test
	public void miniTest76() throws IOException, InterruptedException {
		super.miniTest76();

		
		assertTrue("No hole, iteration # should be 0", stat.getIterations() == 0);
	}

	@Test
	public void miniTest77() throws IOException, InterruptedException {
		super.miniTest77();

		
		assertTrue("No hole, iteration # should be 0", stat.getIterations() == 0);
	}

	@Test
	public void miniTest78() throws IOException, InterruptedException {
		super.miniTest78();

		assertTrue("AllZeroOracle works, should iterate 0 times", stat.getIterations() == 0);
	}

	@Test
	public void miniTest79() throws IOException, InterruptedException {
		super.miniTest79();

		

		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue((sorter.getHoleValueByOrder(0) == 4));
	}

	@Test
	public void miniTest80() throws IOException, InterruptedException {
		super.miniTest80();

		
		assertTrue("No hole, iteration # should be 0", stat.getIterations() == 0);
	}

	@Test
	public void miniTest81() throws IOException, InterruptedException {
		super.miniTest81();

		

		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue((sorter.getHoleValueByOrder(0) == 1));
	}

	@Test
	public void miniTest82() throws IOException, InterruptedException {
	    super.miniTest82();
	}

	@Test
	public void miniTest83() throws IOException, InterruptedException {
		super.miniTest83();

		

	}

	@Test
	public void miniTest84() throws IOException, InterruptedException {
		super.miniTest84();

		

	}

	 @Test
	 public void miniTest85() throws IOException, InterruptedException {
		 super.miniTest85();
			
		 HoleSorter sorter = new HoleSorter(oracle);
		 assertTrue((sorter.getHoleValueByOrder(0) == 2));
		 assertTrue((sorter.getHoleValueByOrder(1) != 0));
		 assertTrue((sorter.getHoleValueByOrder(2) != 0));
		 
	 }

	@Test
	public void miniTest86() throws IOException, InterruptedException {
		super.miniTest86();

		

	}

	@Test
	public void miniTest87() throws IOException, InterruptedException {
		super.miniTest87();

		
	}
	
	@Test
	public void miniTest88() throws IOException, InterruptedException {
		super.miniTest88();
		
		
		// mod
	}

	@Test
	public void miniTest89() throws IOException, InterruptedException {
		super.miniTest89();

		

	}

	@Test
	public void miniTest90() throws IOException, InterruptedException {
		super.miniTest90();

		

		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue((sorter.getHoleValueByOrder(0) != 0));
	}

	@Test
	public void miniTest91() throws IOException, InterruptedException {
		super.miniTest91();
	}

	 @Test
	 public void miniTest92() throws IOException, InterruptedException {
	     super.miniTest92();
	 }
	
	@Test
	public void miniTest93() throws IOException, InterruptedException {
		super.miniTest93();
	}

	@Test
	public void miniTest94() throws IOException, InterruptedException {
		super.miniTest94();

		

		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue((sorter.getHoleValueByOrder(0) != 0));
	}

	@Test
	public void miniTest95() throws IOException, InterruptedException {
		super.miniTest95();

		

	}

	@Test
	public void miniTest96() throws IOException, InterruptedException {
		super.miniTest96();

		

	}

	@Test
	public void miniTest97() throws IOException, InterruptedException {
		super.miniTest97();

		

		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue((sorter.getHoleValueByOrder(0) != 0));
	}

	@Test
	public void miniTest98() throws IOException, InterruptedException {
		super.miniTest98();

		

	}

	@Test
	public void miniTest99() throws IOException, InterruptedException {
		super.miniTest99();

		

	}

	@Test
	public void miniTestb100() throws IOException, InterruptedException {
		super.miniTestb100();

		
	}
	
	@Test
	public void miniTestb101() throws IOException, InterruptedException {
		super.miniTestb101();
			
	}

	@Test
	public void miniTestb102() throws IOException, InterruptedException {
		super.miniTestb102();

		

		// big test
		// hard to verify
	}

	@Test
	public void miniTestb103() throws IOException, InterruptedException {
		super.miniTestb103();

		

		// big test
		// hard to verify
	}

	@Test
	public void miniTestb104() throws IOException, InterruptedException {
		super.miniTestb104();

		

		// big test
		// hard to verify
	}
	
	@Test
	public void miniTestb105() throws IOException, InterruptedException {
		super.miniTestb105();
		
	}
	
	@Test
	public void miniTestb106() throws IOException, InterruptedException {
		super.miniTestb106();
		

		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue((sorter.getHoleValueByOrder(0) == 3));
	}

	@Test
	public void miniTestb107() throws IOException, InterruptedException {
		super.miniTestb107();

		

	}

	@Test
	public void miniTestb108() throws IOException, InterruptedException {
		super.miniTestb108();

		

		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue(sorter.getHoleValueByOrder(0) > 1); // t >= N
	}

	@Test
	public void miniTestb109() throws IOException, InterruptedException {
		super.miniTestb109();

		

		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue(sorter.getHoleValueByOrder(0) > 1); // t >= N
	}

	@Test
	public void miniTestb110() throws IOException, InterruptedException {
		super.miniTestb110();

		

		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue(sorter.getHoleValueByOrder(0) >= 5); // t >= N
	}

	@Test
	public void miniTestb111() throws IOException, InterruptedException {
		super.miniTestb111();

		

	}

	@Test
	public void miniTestb112() throws IOException, InterruptedException {
		super.miniTestb112();

		

	}

	@Test
	public void miniTestb113() throws IOException, InterruptedException {
		super.miniTestb113();

		

	}

	@Test
	public void miniTestb114() throws IOException, InterruptedException {
		super.miniTestb114();

		

		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue((sorter.getHoleValueByOrder(0) == 5));
	}

	@Test
	public void miniTestb116() throws IOException, InterruptedException {
		super.miniTestb116();

		

	}

	@Test
	public void miniTestb117() throws IOException, InterruptedException {
		super.miniTestb117();
	}

	@Test
	public void miniTestb118() throws IOException, InterruptedException {
		super.miniTestb118();

		
	}
	
	@Test
	public void miniTestb120() throws IOException, InterruptedException {
		super.miniTestb120();
		
	}

	@Test
	public void miniTestb121() throws IOException, InterruptedException {
		super.miniTestb121();

		

		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue((sorter.getHoleValueByOrder(0) != 0));
	}

	@Test
	public void miniTestb122() throws IOException, InterruptedException {
		super.miniTestb122();

		

	}

	@Test
	public void miniTestb123() throws IOException, InterruptedException {
		super.miniTestb123();

		

	}

	@Test
	public void miniTestb124() throws IOException, InterruptedException {
		super.miniTestb124();

		

	}

	@Test
	public void miniTestb125() throws IOException, InterruptedException {
		super.miniTestb125();

		

		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue((sorter.getHoleValueByOrder(0) == 3));
	}

	@Test
	public void miniTestb126() throws IOException, InterruptedException {
		super.miniTestb126();

		

		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue((sorter.getHoleValueByOrder(0) == 3));
	}

	@Test
	public void miniTestb127() throws IOException, InterruptedException {
		super.miniTestb127();

		

	}

	@Test
	public void miniTestb128() throws IOException, InterruptedException {
		super.miniTestb128();

		

	}

	@Test
	public void miniTestb129() throws IOException, InterruptedException {
		super.miniTestb129();

		

		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue((sorter.getHoleValueByOrder(0) != 0));
		assertTrue((sorter.getHoleValueByOrder(1) != 0));
	}

	@Test
	public void miniTestb130() throws IOException, InterruptedException {
		super.miniTestb130();

		

		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue((sorter.getHoleValueByOrder(0) != 0));
		assertTrue((sorter.getHoleValueByOrder(0) != 0));
	}

	@Test
	public void miniTestb131() throws IOException, InterruptedException {
		super.miniTestb131();

		

	}

	@Test
	public void miniTestb132() throws IOException, InterruptedException {
		super.miniTestb132();

		

	}

	@Test
	public void miniTestb133() throws IOException, InterruptedException {
		super.miniTestb133();

		

	}

	@Test
	public void miniTestb134() throws IOException, InterruptedException {
		super.miniTestb134();

		

	}

	@Test
	public void miniTestb135() throws IOException, InterruptedException {
		super.miniTestb135();

		

	}

	@Test
	public void miniTestb136() throws IOException, InterruptedException {
		super.miniTestb136();

		

		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue((sorter.getHoleValueByOrder(0) != 0));
	}

	@Test
	public void miniTestb137() throws IOException, InterruptedException {
		super.miniTestb137();

		

	}

	@Test
	public void miniTestb138() throws IOException, InterruptedException {
		super.miniTestb138();

		

	}

	@Test
	public void miniTestb139() throws IOException, InterruptedException {
		super.miniTestb139();

		

	}

	@Test
	public void miniTestb140() throws IOException, InterruptedException {
		super.miniTestb140();

		

		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue((sorter.getHoleValueByOrder(0) == 2 - 
		        sorter.getHoleValueByOrder(1)));
	}

	@Test
	public void miniTestb141() throws IOException, InterruptedException {
		super.miniTestb141();
		// an infinite loop after reducing the num bits for int to 8
	}

	@Test
	public void miniTestb142() throws IOException, InterruptedException {
		super.miniTestb142();

		

	}

	@Test
	public void miniTestb143() throws IOException, InterruptedException {
		super.miniTestb143();

		

	}

	@Test
	public void miniTestb144() throws IOException, InterruptedException {
		super.miniTestb144();

		

		// reorder block
		// too hard to verify in detail
	}

	@Test
	public void miniTestb145() throws IOException, InterruptedException {
		super.miniTestb145();

		

	}

	@Test
	public void miniTestb146() throws IOException, InterruptedException {
		super.miniTestb146();

		

		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue((sorter.getHoleValueByOrder(0) == 1));
	}

	@Test
	public void miniTestb147() throws IOException, InterruptedException {
		super.miniTestb147();

		HoleSorter sorter = new HoleSorter(oracle);
//		assertTrue((sorter.getHoleValueByOrder(0) == 0));
//		assertTrue((sorter.getHoleValueByOrder(1) == 0));
//		assertTrue((sorter.getHoleValueByOrder(2) == 2));
//		assertTrue((sorter.getHoleValueByOrder(3) == 2));

		// takes a long time to verify because of int32. If changed to int20,
		// it's very quick
	}

	@Test
	public void miniTestb148() throws IOException, InterruptedException {
		super.miniTestb148();

		
	}

	@Test
	public void miniTestb149() throws IOException, InterruptedException {
		super.miniTestb149();

		
	}

	@Test
	public void miniTestb150() throws IOException, InterruptedException {
		super.miniTestb150();

		
	}

	@Test
	public void miniTestb151() throws IOException, InterruptedException {
		super.miniTestb151();

		
	}

	@Test
	public void miniTestb152() throws IOException, InterruptedException {
		super.miniTestb152();

		
	}

	@Test
	public void miniTestb153() throws IOException, InterruptedException {
		super.miniTestb153();

		

		// reorder block
		// too hard to verify in detail
	}

	@Test
	public void miniTestb154() throws IOException, InterruptedException {
		super.miniTestb154();

		
	}

	@Test
	public void miniTestb155() throws IOException, InterruptedException {
		super.miniTestb155();

		
	}

	@Test
	public void miniTestb156() throws IOException, InterruptedException {
		super.miniTestb156();

		

		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue(sorter.getHoleValueByOrder(0) == 2);
		assertTrue(sorter.getHoleValueByOrder(1) == 1);
	}

	@Test
	public void miniTestb157() throws IOException, InterruptedException {
		super.miniTestb157();

		
	}

	@Test
	public void miniTestb158() throws IOException, InterruptedException {
		super.miniTestb158();

		

		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue((sorter.getHoleValueByOrder(0) == 3));
		assertTrue((sorter.getHoleValueByOrder(1) == 5));
		assertTrue((sorter.getHoleValueByOrder(2) == 0));
	}

	@Test
	public void miniTestb159() throws IOException, InterruptedException {
		super.miniTestb159();

		

		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue((sorter.getHoleValueByOrder(0) == 3));
		assertTrue((sorter.getHoleValueByOrder(1) == 5));
	}

	@Test
	public void miniTestb160() throws IOException, InterruptedException {
		super.miniTestb160();
		
		
		// mod
	}
	@Test
	public void miniTestb161() throws IOException, InterruptedException {
		super.miniTestb161();
		
		
		// mod
	}
	
	@Test
	public void miniTestb162() throws IOException, InterruptedException {
		super.miniTestb162();

		

		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue((sorter.getHoleValueByOrder(0) == 2));
	}

	@Test
	public void miniTestb163() throws IOException, InterruptedException {
		super.miniTestb163();

		

		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue((sorter.getHoleValueByOrder(0) == 2));
	}

	@Test
	public void miniTestb164() throws IOException, InterruptedException {
		super.miniTestb164();

		
		// too complicated to express
	}

	@Test
	public void miniTestb165() throws IOException, InterruptedException {
		super.miniTestb165();

	}

	@Test
	public void miniTestb166() throws IOException, InterruptedException {
		super.miniTestb166();

		

		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue((sorter.getHoleValueByOrder(0) == 4));
		assertTrue((sorter.getHoleValueByOrder(1) == 2));
		assertTrue((sorter.getHoleValueByOrder(2) == 0));
	}

	@Test
	public void miniTestb167() throws IOException, InterruptedException {
		super.miniTestb167();

		
	}

	@Test
	public void miniTestb168() throws IOException, InterruptedException {
		super.miniTestb168();

		

	}

	@Test
	public void miniTestb169() throws IOException, InterruptedException {
		super.miniTestb169();

		

		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue((sorter.getHoleValueByOrder(0) == 1));
	}

	@Test
	public void miniTestb170() throws IOException, InterruptedException {
		super.miniTestb170();

		// H___0 = 0
		// H___1 = 1
		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue((sorter.getHoleValueByOrder(0) == 0));
		assertTrue((sorter.getHoleValueByOrder(1) == 1));
	}
	
	@Test
	public void miniTestb171() throws IOException, InterruptedException {
		super.miniTestb171();
	}

	@Test
	public void miniTestb172() throws IOException, InterruptedException {
		super.miniTestb172();
	}

	@Test
	public void miniTestb173() throws IOException, InterruptedException {
		super.miniTestb173();

		HoleSorter sorter = new HoleSorter(oracle);
		assertTrue(sorter.getHoleValueByOrder(0) == 3); // choose f.n.p.v
	}

	@Test
	public void miniTestb174() throws IOException, InterruptedException {
		super.miniTestb174();
	}

	@Test
	public void miniTestb175() throws IOException, InterruptedException {
		super.miniTestb175();
	}

	@Test
	public void miniTestb176() throws IOException, InterruptedException {
		super.miniTestb176();		
	}

	@Test
	public void miniTestb177() throws IOException, InterruptedException {
	    super.miniTestb177();
	}

	@Test
	public void miniTestb178() throws IOException, InterruptedException {
		super.miniTestb178();
	}

	@Test
	public void miniTestb179() throws IOException, InterruptedException {
	    super.miniTestb179();
	}
}
