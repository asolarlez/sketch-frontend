package sketch.compiler.seq;

import java.io.IOException;
import java.util.HashMap;

import org.junit.Test;

import sketch.compiler.smt.TestHarness;

public abstract class StencilShowcase extends TestHarness {
    protected abstract HashMap<String, String> initCmdArgs(String string);

    protected abstract void runOneTest(String[] args) throws IOException,
            InterruptedException;

    @Test
    public void rb3d() throws IOException, InterruptedException {
        HashMap<String, String> argsMap = initCmdArgs("rb3d.sk");
        runOneTest(toArgArray(argsMap));
    }

    @Test
    public void rb3dSimpleOdd() throws IOException, InterruptedException {
        HashMap<String, String> argsMap = initCmdArgs("rb3dSimpleOdd.sk");
        runOneTest(toArgArray(argsMap));
    }

    @Test
    public void cacheObv1d4() throws IOException, InterruptedException {
        HashMap<String, String> argsMap = initCmdArgs("cacheObv1d4.sk");
        runOneTest(toArgArray(argsMap));
    }

    @Test
    public void distribute2d() throws IOException, InterruptedException {
        HashMap<String, String> argsMap = initCmdArgs("distribute2d.sk");
        runOneTest(toArgArray(argsMap));
    }

    @Test
    public void mpInterpBlock() throws IOException, InterruptedException {
        HashMap<String, String> argsMap = initCmdArgs("mgInterpBlock.sk");
        runOneTest(toArgArray(argsMap));
    }

    @Test
    public void mgInterpFast() throws IOException, InterruptedException {
        HashMap<String, String> argsMap = initCmdArgs("mgInterpFast.sk");
        runOneTest(toArgArray(argsMap));
    }

    @Test
    public void timeSkewing3() throws IOException, InterruptedException {
        HashMap<String, String> argsMap = initCmdArgs("timeSkewing3.sk");
        runOneTest(toArgArray(argsMap));
    }

    @Test
    public void timeSkewing4() throws IOException, InterruptedException {
        HashMap<String, String> argsMap = initCmdArgs("timeSkewing4.sk");
        runOneTest(toArgArray(argsMap));
    }
}
