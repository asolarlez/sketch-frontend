package sketch.compiler.smt.z3;

import java.util.HashMap;

public class SketchShowcaseCSE2CanonFHBlastBV extends
        sketch.compiler.smt.tests.SketchShowcaseCSE2CanonFHBlastBV
{
    protected HashMap<String, String> initCmdArgs(String input) {
        HashMap<String, String> argsMap = super.initCmdArgs(input);
        argsMap.put("--backend", "z3");
        
        argsMap.put("--verbosity", "4");
//      argsMap.put("--showphase", "lowering");
        System.out.print(input + "\tz3-cse2cfhblastbv");
        return argsMap;
    }
}
