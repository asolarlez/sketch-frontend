package sketch.compiler.smt.yices2;

import java.io.IOException;
import java.util.HashMap;

import org.junit.Assert;

public class StenRegressionCanonFHBlastBV extends
        sketch.compiler.smt.tests.StenRegressionTests
{
    protected HashMap<String, String> initCmdArgs(String input) {
        HashMap<String, String> argsMap = super.initCmdArgs(input);
        argsMap.put("--backend", "yices2");
        
         argsMap.put("--verbosity", "4");
        // argsMap.put("--showphase", "parse");
        System.out.print(input + "\tyices2");
        return argsMap;
    }
    
    @Override
    public void miniTest2() throws IOException, InterruptedException {
        Assert.fail("takes a long time to fail");
        super.miniTest2();
    }
    
    @Override
    public void miniTest25() throws IOException, InterruptedException {
        Assert.fail("Max number of statement reached inside stencil transformations");
        super.miniTest25();
    }
    
}
