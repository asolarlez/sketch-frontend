package sketch.compiler.smt.z3;

import java.util.HashMap;

public class SketchShowcaseCanonFHTOABV extends
        sketch.compiler.smt.tests.SketchShowcaseCanonFHTOABV
{
    protected HashMap<String, String> initCmdArgs(String input) {
        HashMap<String, String> argsMap = super.initCmdArgs(input);
        argsMap.put("--backend", "z3");
        
        argsMap.put("--verbosity", "0");
//      argsMap.put("--showphase", "lowering");
        System.out.print(input + "\tz3-cfhtoabv");
        return argsMap;
    }
}
