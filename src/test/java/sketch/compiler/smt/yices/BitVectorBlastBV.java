package sketch.compiler.smt.yices;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashMap;

import org.junit.Test;

import sketch.compiler.main.seq.SequentialSMTSketchMain;
import sketch.compiler.smt.TestHarness;

public class BitVectorBlastBV extends sketch.compiler.smt.tests.BitVectorBlastBV {

	protected HashMap<String, String> initCmdArgs(String input) {
		HashMap<String, String> argsMap = super.initCmdArgs(input);
		argsMap.put("--backend", "yices");
		
		// argsMap.put("--verbosity", "4");
		// argsMap.put("--showphase", "parse");
		System.out.print(input + "\tyices");
		return argsMap;
	}
}
