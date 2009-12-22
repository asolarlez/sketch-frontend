package sketch.compiler.smt.yices2;

import java.util.HashMap;

public class SketchShowcaseBlastBV extends
        sketch.compiler.smt.tests.SketchShowcaseBlastBV
{
    
    protected HashMap<String, String> initCmdArgs(String input) {
        HashMap<String, String> argsMap = super.initCmdArgs(input);
        argsMap.put("--backend", "yices2");
        
         argsMap.put("--verbosity", "0");
//         argsMap.put("--showphase", "lowering");
        System.out.print(input + "\tyices2-blastbv");
        return argsMap;
    }
    
//    @Override
//    public void SpMV_CSR1_N5_BIT() throws IOException, InterruptedException {
//        initCmdArgs("SpMV-CSR1-N5-BIT.sk");
//        mStatus = "TIMEOUT";
//    }
}
