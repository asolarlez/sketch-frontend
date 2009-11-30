package sketch.compiler.smt.stp;

import java.util.HashMap;

public class SketchShowcaseTOABV extends
        sketch.compiler.smt.tests.SketchShowcaseTOABV
{
    
    @Override
    protected HashMap<String, String> initCmdArgs(String input) {
        HashMap<String, String> argsMap = super.initCmdArgs(input);
        argsMap.put("--backend", "stp");
        
        argsMap.put("--verbosity", "0");
//        argsMap.put("--showphase", "lowering");
        System.out.print(input + "\tstp-toa");
        return argsMap;
    }
}
