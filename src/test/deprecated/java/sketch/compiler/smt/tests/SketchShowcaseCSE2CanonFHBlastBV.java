package sketch.compiler.smt.tests;

import java.util.HashMap;

public class SketchShowcaseCSE2CanonFHBlastBV extends
        SketchShowcaseCSECanonFHBlastBV
{
    @Override
    protected HashMap<String, String> initCmdArgs(String input) {
     
        HashMap<String, String> argsMap = super.initCmdArgs(input);
        argsMap.put("--cse2", null);
        return argsMap;
    }
}
