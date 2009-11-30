package sketch.compiler.smt.tests;

import java.util.HashMap;

public class LanguageBasicFHBlastBV extends LanguageBasicBlastBV {
    
    @Override
    protected HashMap<String, String> initCmdArgs(String input) {
     
        HashMap<String, String> argsMap = super.initCmdArgs(input);
        argsMap.put("--funchash", null);
        return argsMap;
    }
}
