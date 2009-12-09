package sketch.compiler.smt.tests;

import java.io.IOException;
import java.util.HashMap;

import org.junit.After;

import sketch.compiler.main.sten.StencilSmtSketchMain;
import sketch.compiler.seq.StencilShowcase;
import sketch.compiler.smt.partialeval.NodeToSmtVtype;

public class StencilOptBlastBV extends StencilShowcase {
    
    @Override
    public void init() { }
    
    @Override
    protected HashMap<String, String> initCmdArgs(String input) {
        String inputPath = "src/test/sk/smt/stenShowcase/" + input;
        HashMap<String, String> argsMap = new HashMap<String, String>();
        argsMap.put("--smtpath", System.getenv("smtpath"));
        // argsMap.put("--arrayOOBPolicy", "assertions");
        // argsMap.put("--heapsize", "10");
        argsMap.put("--intbits", "8");
        argsMap.put("--outputdir", "output/");        
        argsMap.put("--keeptmpfiles", null);

        
        
        
        
        
        argsMap.put("--verbosity", "0");
        
        argsMap.put(inputPath, null);
        
        System.out.print(input + "\tbase");
        return argsMap;
    }

    @Override
    protected void runOneTest(String[] args) throws IOException,
            InterruptedException
    {
        
        StencilSmtSketchMain main = new StencilSmtSketchMain(args);
        main.parseProgram();
        main.processing();
        main.generateDAG();
        stat = main.getSolutionStat();
    }
    
    @After
    public void printStat() {
        System.out.println("\t" +
                stat.getLong(NodeToSmtVtype.FUNC_INLINED) + "\t" +
                stat.getLong(NodeToSmtVtype.SH_USED) + "\t" +
                stat.getLong(NodeToSmtVtype.CACHE_SIZE) + "\t" +
                stat.getLong(NodeToSmtVtype.CACHE_USED));
    }
}
