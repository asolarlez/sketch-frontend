package sketch.compiler.smt.stpyices;

import java.util.HashMap;

public class SketchShowcaseBlastBV extends
        sketch.compiler.smt.tests.SketchShowcaseBlastBV
{
    @Override
    protected HashMap<String, String> initCmdArgs(String input) {
        HashMap<String, String> argsMap = super.initCmdArgs(input);
        argsMap.put("--backend", "stpyices2");
        
        argsMap.put("--verbosity", "0");
//      argsMap.put("--showphase", "lowering");
        System.out.print(input + "\tstpyices2-blastbv");
        return argsMap;
    }
}
