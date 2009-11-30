package sketch.compiler.smt.tests;

import java.util.HashMap;

public class SketchRegressionTOABV extends SketchRegressionBlastBV{
    
    @Override
    protected HashMap<String, String> initCmdArgs(String input) {
     
        HashMap<String, String> argsMap = super.initCmdArgs(input);
        argsMap.put("--theoryofarray", null);
        return argsMap;
    }
}
