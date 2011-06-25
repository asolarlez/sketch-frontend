package sketch.compiler.smt.tests;

import java.util.HashMap;

public class SketchRegressionCanonFHBlastBV extends SketchRegressionFHBlastBV {
    
    @Override
    protected HashMap<String, String> initCmdArgs(String input) {
     
        HashMap<String, String> argsMap = super.initCmdArgs(input);
        argsMap.put("--canon", null);
        return argsMap;
    }
}
