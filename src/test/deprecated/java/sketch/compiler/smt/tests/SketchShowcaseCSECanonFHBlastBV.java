package sketch.compiler.smt.tests;

import java.util.HashMap;

public class SketchShowcaseCSECanonFHBlastBV extends
        SketchShowcaseCanonFHBlastBV
{
    @Override
    protected HashMap<String, String> initCmdArgs(String input) {
     
        HashMap<String, String> argsMap = super.initCmdArgs(input);
        argsMap.put("--cse", null);
        return argsMap;
    }
}
