package sketch.compiler.smt;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import org.junit.After;
import org.junit.Before;

import sketch.compiler.CommandLineParamManager;
import sketch.compiler.smt.CEGISLoop.CEGISStat;
import sketch.compiler.smt.partialeval.SmtValueOracle;

public class TestHarness {
	
	protected final static String tmpDirStr = System.getenv("tmpdir");
	protected File tmpDir = new File(tmpDirStr);

	protected CEGISStat stat;
	protected SmtValueOracle oracle;

	@Before
	public void init() {	
		if (!tmpDir.exists())
			tmpDir.mkdir();
	}
	
	@After
	public void cleanup() {
		CommandLineParamManager.getParams().clear();
	}
	
	protected String[] toArgArray(HashMap<String,String> argsMap) {
		ArrayList<String> array = new ArrayList<String>();
		
		String input = null;
		for (String key : argsMap.keySet()) {
			if (!key.startsWith("-")) {
				input = key;
				continue;
			}
			String value = argsMap.get(key);
			
			array.add(key);
			if (value != null)
				array.add(value);
		}
		array.add(input); // input has to be the last element
		
		String[] typeDummy = new String[1];
		return array.toArray(typeDummy);
	}

}
