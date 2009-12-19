package sketch.compiler.smt.z3;

import java.io.IOException;
import java.util.HashMap;

public class SketchShowcaseBlastBV extends
        sketch.compiler.smt.tests.SketchShowcaseBlastBV
{
    protected HashMap<String, String> initCmdArgs(String input) {
        HashMap<String, String> argsMap = super.initCmdArgs(input);
        argsMap.put("--backend", "z3");
        
        argsMap.put("--verbosity", "0");
//      argsMap.put("--showphase", "lowering");
        System.out.print(input + "\tz3-blastbv");
        return argsMap;
    }
    
    @Override
    public void xpose() throws IOException, InterruptedException {
        initCmdArgs("xpose.sk");
        mStatus = "TIMEOUT";
    }
}
