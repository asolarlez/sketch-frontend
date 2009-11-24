package sketch.compiler.smt.stp;

import java.util.HashMap;

public class Opt extends sketch.compiler.smt.tests.Opt {

    protected HashMap<String, String> initCmdArgs(String input) {
        HashMap<String, String> argsMap = super.initCmdArgs(input);
        argsMap.put("--backend", "stp");
        
         argsMap.put("--verbosity", "6");
         argsMap.put("--showphase", "lowering");
        System.out.print(input + "\tstp");
        return argsMap;
    }
    
}
