package sketch.compiler.smt.tests;

import java.util.HashMap;

public class SketchRegressionCanonFHTOABV extends SketchRegressionTOABV {

    @Override
    protected HashMap<String, String> initCmdArgs(String input) {
     
        HashMap<String, String> argsMap = super.initCmdArgs(input);
        argsMap.put("--funchash", null);
        argsMap.put("--canon", null);
        return argsMap;
    }
}
