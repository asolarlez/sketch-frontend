package sketch.compiler.smt.cvc3;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.junit.Test;

import sketch.compiler.main.seq.SequentialSMTSketchMain;
import sketch.compiler.smt.HoleSorter;
import sketch.compiler.smt.SmtOracle;
import sketch.compiler.smt.TestHarness;
import sketch.compiler.smt.partialeval.NodeToSmtValue;
import sketch.compiler.smt.partialeval.SmtValueOracle;

public class LanguageBasicBlastInt extends TestHarness {

	protected final static String tmpDirStr = "/tmp/sketch";
	protected File tmpDir = new File(tmpDirStr);

	protected HashMap<String, String> initCmdArgs(String inputPath) {
		HashMap<String, String> argsMap = new HashMap<String, String>();
		argsMap.put("--smtpath", System.getenv("smtpath"));

		argsMap.put("--arrayOOBPolicy", "assertions");
		argsMap.put("--heapsize", "10");
		argsMap.put("--inbits", "32");
		argsMap.put("--keeptmpfiles", null);
		argsMap.put("--verbosity", "0");
		argsMap.put("--outputdir", "output/");
		argsMap.put("--tmpdir", tmpDirStr);
		argsMap.put("--showphase", "lowering");
		argsMap.put("--trace", null);
		argsMap.put(inputPath, null);
		return argsMap;
	}

	

}
