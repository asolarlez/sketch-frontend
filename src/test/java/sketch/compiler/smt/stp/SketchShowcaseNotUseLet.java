package sketch.compiler.smt.stp;

import java.io.IOException;
import java.util.HashMap;

import org.junit.Assert;
import org.junit.Test;

import sketch.compiler.smt.tests.SketchShowcase;

public class SketchShowcaseNotUseLet extends SketchShowcase {

    @Override
    protected HashMap<String, String> initCmdArgs(String input) {
        HashMap<String, String> argsMap = super.initCmdArgs(input);
        argsMap.put("--backend", "stp");
        
        argsMap.put("--bv", null);
        
        argsMap.put("--funchash", null);
        argsMap.put("--verbosity", "0");
//      argsMap.put("--showphase", "lowering");
        System.out.print(input + "\tstp-nolet");
        
        return argsMap;
    }
    
    @Test
    public void SpMV_COO1_N2_INT() throws IOException, InterruptedException {
        Assert.fail("STP not showing full model");
    }
}
