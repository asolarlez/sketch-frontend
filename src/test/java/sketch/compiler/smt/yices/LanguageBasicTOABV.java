package sketch.compiler.smt.yices;

import java.util.HashMap;

public class LanguageBasicTOABV extends
        sketch.compiler.smt.tests.LanguageBasicBlastBV
{
    protected HashMap<String, String> initCmdArgs(String input) {
        HashMap<String, String> argsMap = super.initCmdArgs(input);
        argsMap.put("--backend", "yices");
        argsMap.put("--theoryofarray", null);
        argsMap.put("--verbosity", "4");
        argsMap.put("--showphase", "lowering");
        System.out.print(input + "\tyices");
        return argsMap;
    }
}
