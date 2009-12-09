package sketch.compiler.smt.tests;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashMap;

import org.junit.After;

import sketch.compiler.main.sten.StencilSmtSketchMain;
import sketch.compiler.smt.CEGISLoop;
import sketch.compiler.smt.SolverFailedException;
import sketch.compiler.smt.partialeval.NodeToSmtVtype;
import sketch.compiler.smt.partialeval.SmtValueOracle;

public class StencilShowcase extends sketch.compiler.seq.StencilShowcase {
    protected String mStatus;

    protected HashMap<String, String> initCmdArgs(String input) {
        String inputPath = "src/test/sk/smt/stenShowcase/" + input;
        HashMap<String, String> argsMap = new HashMap<String, String>();
        argsMap.put("--smtpath", System.getenv("smtpath"));
        // argsMap.put("--arrayOOBPolicy", "assertions");
        // argsMap.put("--heapsize", "10");
        argsMap.put("--intbits", "8");
        argsMap.put("--outputdir", "output/");
        argsMap.put("--tmpdir", tmpDirStr);
        argsMap.put("--keeptmpfiles", null);
        // argsMap.put("--trace", null);
        argsMap.put(inputPath, null);
        return argsMap;
    }

    public void runOneTest(String[] args) throws IOException,
            InterruptedException
    {
        stat = null;
        oracle = null;
        try {
            StencilSmtSketchMain main = new StencilSmtSketchMain(args);
            assertTrue(main.runBeforeGenerateCode());
            oracle = (SmtValueOracle) main.getOracle();
            stat = main.getSolutionStat();
        } catch (SolverFailedException e) {
            mStatus = "NO_MODEL";
            stat = null;
        }
        
    }

    @After
    public void printTiming() {
        if (stat == null)
            System.out.println("\t" + mStatus);
        else
            System.out.println("\t"
                + (stat.getLong(CEGISLoop.SYNTHESIS_TIME) + stat
                        .getLong(CEGISLoop.VERIFICATION_TIME)) + "\t"
                + stat.getLong(CEGISLoop.CEGIS_ITR) + "\t"
                + stat.getLong(NodeToSmtVtype.FUNC_INLINED));
    }
}
