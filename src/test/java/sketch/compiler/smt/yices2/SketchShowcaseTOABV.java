package sketch.compiler.smt.yices2;

import java.io.IOException;
import java.util.HashMap;

public class SketchShowcaseTOABV extends
        sketch.compiler.smt.tests.SketchShowcaseTOABV
{
    
    protected HashMap<String, String> initCmdArgs(String input) {
        HashMap<String, String> argsMap = super.initCmdArgs(input);
        argsMap.put("--backend", "yices2");
        
         argsMap.put("--verbosity", "0");
//         argsMap.put("--showphase", "lowering");
        System.out.print(input + "\tyices2-toabv");
        return argsMap;
    }
    
    @Override
    public void Pollard() throws IOException, InterruptedException {
        initCmdArgs("Pollard.sk");
        mStatus = "TIMEOUT";
    }
    
    @Override
    public void SpMV_COO1_N2_BIT() throws IOException, InterruptedException {
        initCmdArgs("SpMV-COO1-N2-BIT.sk");
        mStatus = "TIMEOUT";
    }
    
    @Override
    public void SpMV_COO1_N3_INT() throws IOException, InterruptedException {
        initCmdArgs("SpMV-COO1-N3-INT.sk");
        mStatus = "TIMEOUT";
    }
    
    @Override
    public void SpMV_DIA1_N2_INT() throws IOException, InterruptedException {
        
        initCmdArgs("SpMV-DIA1-N2-INT.sk");
        mStatus = "TIMEOUT";
    }
    
    @Override
    public void SpMV_DIA2_N4_BIT() throws IOException, InterruptedException {
        initCmdArgs("SpMV-DIA2-N4-BIT.sk");
        mStatus = "TIMEOUT";
    }
    
    @Override
    public void xpose() throws IOException, InterruptedException {
        initCmdArgs("xpose.sk");
        mStatus = "TIMEOUT";
    }
    
    @Override
    public void SpMV_DIA1_N3_INT() throws IOException, InterruptedException {
        initCmdArgs("SpMV-DIA1-N3-INT.sk");
        mStatus = "TIMEOUT";
    }
    
    
}
