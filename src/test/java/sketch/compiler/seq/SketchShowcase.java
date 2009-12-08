package sketch.compiler.seq;

import java.io.IOException;
import java.util.HashMap;

import org.junit.Test;

import sketch.compiler.smt.TestHarness;

public abstract class SketchShowcase extends TestHarness {

	protected abstract HashMap<String, String> initCmdArgs(String string);
	protected abstract void runOneTest(String[] args) throws IOException, InterruptedException;
	
	
	
	// DIA-2
	@Test(timeout=15*60*1000)
	public void SpMV_DIA2_N2_BIT() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("SpMV-DIA2-N2-BIT.sk");
		runOneTest(toArgArray(argsMap));
	}
	
	@Test(timeout=15*60*1000)
	public void SpMV_DIA2_N3_BIT() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("SpMV-DIA2-N3-BIT.sk");
		runOneTest(toArgArray(argsMap));
	}

	
	
//	@Test(timeout=15*60*1000)
//	public void SpMV_DIA2_N5_BIT() throws IOException, InterruptedException {
//		HashMap<String, String> argsMap = initCmdArgs("SpMV-DIA2-N5-BIT.sk");
//		runOneTest(toArgArray(argsMap));
//	}
//	
//	@Test(timeout=15*60*1000)
//	public void SpMV_DIA2_N6_BIT() throws IOException, InterruptedException {
//		HashMap<String, String> argsMap = initCmdArgs("SpMV-DIA2-N6-BIT.sk");
//		runOneTest(toArgArray(argsMap));
//	}
	
	// DIA-1
	
	
	@Test(timeout=15*60*1000)
	public void SpMV_DIA1_N2_BIT() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("SpMV-DIA1-N2-BIT.sk");
		runOneTest(toArgArray(argsMap));
	}

	
	
	@Test(timeout=15*60*1000)
	public void SpMV_DIA1_N3_BIT() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("SpMV-DIA1-N3-BIT.sk");
		runOneTest(toArgArray(argsMap));
	}
	
	
	
	// CSR
	@Test(timeout=15*60*1000)
	public void SpMV_CSR1_N2_BIT() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("SpMV-CSR1-N2-BIT.sk");

		runOneTest(toArgArray(argsMap));
	}
	
	
	
	@Test(timeout=15*60*1000)
	public void SpMV_CSR1_N3_BIT() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("SpMV-CSR1-N3-BIT.sk");

		runOneTest(toArgArray(argsMap));
	}
	
	
	// COO
	
	
	@Test(timeout=15*60*1000)
	public void SpMV_COO1_N2_INT() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("SpMV-COO1-N2-INT.sk");

		runOneTest(toArgArray(argsMap));
	}

	
	@Test
	public void SpMV_COO1_N3_BIT() throws IOException, InterruptedException {
		HashMap<String, String> argsMap = initCmdArgs("SpMV-COO1-N3-BIT.sk");

		runOneTest(toArgArray(argsMap));
	}
	
	@Test
    public void SpMV_CSR1_N2_INT() throws IOException, InterruptedException {
        HashMap<String, String> argsMap = initCmdArgs("SpMV-CSR1-N2-INT.sk");

        runOneTest(toArgArray(argsMap));
    }
    @Test
    public void SpMV_CSR1_N3_INT() throws IOException, InterruptedException {
        HashMap<String, String> argsMap = initCmdArgs("SpMV-CSR1-N3-INT.sk");

        runOneTest(toArgArray(argsMap));
    }
	
	// others
	
	
    
//    @Test
//    public void logcount() throws IOException, InterruptedException {
//        HashMap<String, String> argsMap = initCmdArgs("logcount.sk");
//        runOneTest(toArgArray(argsMap));
//    }
    
	@Test
    public void polynomialInt() throws IOException, InterruptedException {
        HashMap<String, String> argsMap = initCmdArgs("polynomialInt.sk");
        runOneTest(toArgArray(argsMap));
    }
    
	@Test
    public void aesFullStage() throws IOException, InterruptedException {
        HashMap<String, String> argsMap = initCmdArgs("aesFullStage.sk");
        runOneTest(toArgArray(argsMap));
    }
    
//    @Test
//    public void des2() throws IOException, InterruptedException {
//        // STP used > 2GB can't finish
//        HashMap<String, String> argsMap = initCmdArgs("des2.sk");
//        runOneTest(toArgArray(argsMap));
//    }
    
	
	
//    @Test
//    public void tableBasedAddition() throws IOException, InterruptedException {
//        // over 132 iterations, over 1hr
//        HashMap<String, String> argsMap = initCmdArgs("tableBasedAddition.sk");
//        runOneTest(toArgArray(argsMap));
//    }

	/*
	 * Likely timeout
	 */

    @Test
    public void SpMV_COO1_N2_BIT() throws IOException, InterruptedException {
        HashMap<String, String> argsMap = initCmdArgs("SpMV-COO1-N2-BIT.sk");

        runOneTest(toArgArray(argsMap));
    }
	
	@Test
    public void SpMV_DIA1_N2_INT() throws IOException, InterruptedException {
        HashMap<String, String> argsMap = initCmdArgs("SpMV-DIA1-N2-INT.sk");
        runOneTest(toArgArray(argsMap));
    }
    @Test
    public void SpMV_DIA1_N3_INT() throws IOException, InterruptedException {
        HashMap<String, String> argsMap = initCmdArgs("SpMV-DIA1-N3-INT.sk");
        runOneTest(toArgArray(argsMap));
    }
	
	@Test
    public void SpMV_DIA2_N4_BIT() throws IOException, InterruptedException {
        HashMap<String, String> argsMap = initCmdArgs("SpMV-DIA2-N4-BIT.sk");
        runOneTest(toArgArray(argsMap));
    }
    
    @Test
    public void SpMV_COO1_N3_INT() throws IOException, InterruptedException {
        HashMap<String, String> argsMap = initCmdArgs("SpMV-COO1-N3-INT.sk");

        runOneTest(toArgArray(argsMap));
    }
    
    @Test
    public void xpose() throws IOException, InterruptedException {
        HashMap<String, String> argsMap = initCmdArgs("xpose.sk");
        runOneTest(toArgArray(argsMap));
    }
    
    @Test
    public void Pollard() throws IOException, InterruptedException {
        HashMap<String, String> argsMap = initCmdArgs("Pollard.sk");
        runOneTest(toArgArray(argsMap));
    }

}
