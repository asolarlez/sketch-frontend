package sketch.compiler.seq;

import java.io.IOException;
import java.util.HashMap;

import org.junit.Test;

import sketch.compiler.smt.TestHarness;

public abstract class BinTree extends TestHarness {
	
	protected abstract HashMap<String, String> initCmdArgs(String string);
	protected abstract void runOneTest(String[] args) throws IOException, InterruptedException;
	
	@Test
	public void test4() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("test4.sk");

		runOneTest(toArgArray(argsMap));
	}
	
	@Test
	public void test5() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("test5.sk");

		runOneTest(toArgArray(argsMap));
	}
	
	@Test
	public void test6() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("test6.sk");

		runOneTest(toArgArray(argsMap));
	}
	
	@Test
	public void test7() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("test7.sk");

		runOneTest(toArgArray(argsMap));
	}
	
	@Test
	public void test8() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("test8.sk");

		runOneTest(toArgArray(argsMap));
	}
	
	@Test
	public void test9() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("test9.sk");

		runOneTest(toArgArray(argsMap));
	}
	
	@Test
	public void test10() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("test10.sk");

		runOneTest(toArgArray(argsMap));
	}
	
	@Test
	public void test11() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("test11.sk");

		runOneTest(toArgArray(argsMap));
	}
	
	@Test
	public void test12() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("test12.sk");

		runOneTest(toArgArray(argsMap));
	}
	
	@Test
	public void test13() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("test13.sk");

		runOneTest(toArgArray(argsMap));
	}
	
	@Test
	public void test14() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("test14.sk");

		runOneTest(toArgArray(argsMap));
	}
	
	@Test
	public void test15() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("test15.sk");

		runOneTest(toArgArray(argsMap));
	}
	
	@Test
	public void test16() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("test16.sk");

		runOneTest(toArgArray(argsMap));
	}
	

	
}
