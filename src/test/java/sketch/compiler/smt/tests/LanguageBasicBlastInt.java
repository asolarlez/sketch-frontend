package sketch.compiler.smt.tests;

import java.util.HashMap;

public class LanguageBasicBlastInt extends LanguageBasicBlastBV {
    
    protected HashMap<String, String> initCmdArgs(String input) {
        HashMap<String, String> ret = super.initCmdArgs(input);
        ret.put("--modelint", "int");
        return ret;
    }
}
