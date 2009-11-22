package sketch.compiler.smt.yices2;

import java.util.HashMap;

import org.junit.Assert;

public class LanguageBasicTOABV extends
        sketch.compiler.smt.yices.LanguageBasicTOABV
{
    protected HashMap<String, String> initCmdArgs(String input) {
        HashMap<String, String> argsMap = super.initCmdArgs(input);
        argsMap.put("--backend", "yices2");
        argsMap.put("--theoryofarray", null);
        argsMap.put("--verbosity", "4");
        argsMap.put("--showphase", "lowering");
        System.out.print(input + "\tyices2");
        return argsMap;
    }
    
    @Override
    public void testHoleWhile() throws Exception {
        Assert.fail("Yices2 segfault");
        super.testHoleWhile();
    }
    
    @Override
    public void testRegExprObj2() throws Exception {
        Assert.fail("Yices2 segfault");
        super.testRegExprObj2();
    }
    
    @Override
    public void testObjectWhile() throws Exception {
        Assert.fail("Yices2 segfault");
        super.testObjectWhile();
    }
}
