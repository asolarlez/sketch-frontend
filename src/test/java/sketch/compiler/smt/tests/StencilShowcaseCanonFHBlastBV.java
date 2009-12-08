package sketch.compiler.smt.tests;

import java.util.HashMap;

public class StencilShowcaseCanonFHBlastBV 
    extends StencilShowcase {
    
    @Override
    protected HashMap<String, String> initCmdArgs(String input) {
        HashMap<String, String> argsMap = super.initCmdArgs(input);
        argsMap.put("--canon", null);
        argsMap.put("--funchash", null);
        argsMap.put("--modelint", "bv");
        return argsMap;
    }
}
