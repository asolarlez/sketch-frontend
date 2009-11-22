package sketch.compiler.smt.yices2;

import java.util.HashMap;

public class LanguageBasicBlastBV extends sketch.compiler.smt.tests.LanguageBasicBlastBV  {

    protected HashMap<String, String> initCmdArgs(String input) {
        HashMap<String, String> argsMap = super.initCmdArgs(input);
        argsMap.put("--backend", "yices2");
        
         argsMap.put("--verbosity", "4");
         argsMap.put("--showphase", "lowering");
        System.out.print(input + "\tyices2");
        return argsMap;
    }
    
    
}
