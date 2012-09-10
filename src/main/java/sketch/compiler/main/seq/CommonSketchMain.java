package sketch.compiler.main.seq;

import java.util.Vector;

import sketch.compiler.ast.core.Program;
import sketch.compiler.cmdline.SemanticsOptions.ArrayOobPolicy;
import sketch.compiler.cmdline.SolverOptions.SynthSolvers;
import sketch.compiler.cmdline.SolverOptions.VerifSolvers;
import sketch.compiler.main.cmdline.SketchOptions;
import sketch.compiler.passes.printers.SimpleCodePrinter;

public class CommonSketchMain {
    public SketchOptions options;

    public CommonSketchMain(SketchOptions options) {
        this.options = options;
    }

    public static void dump(Program prog) {
        dump(prog, "");
    }

    protected void backendParameters() {
        options.backendOptions = new Vector<String>();
        Vector<String> backendOptions = options.backendOptions;

        // pass all short-style arguments to the backend
        backendOptions.addAll(options.backendArgs);
        backendOptions.add("--bnd-inbits");
        backendOptions.add(""+ options.bndOpts.inbits);
        backendOptions.add("--verbosity");
        backendOptions.add(""+ options.debugOpts.verbosity);
        backendOptions.add("--print-version"); // run by default

        if (options.solverOpts.seed != 0) {
            backendOptions.add("--seed");
            backendOptions.add("" + options.solverOpts.seed);
        }
        if (options.debugOpts.cex) {
            backendOptions.add("--print-cex");
        }
        if (options.solverOpts.synth != SynthSolvers.NOT_SET) {
            // assert false : "solver opts synth need to convert old style command line args";
            backendOptions.add("-synth");
            backendOptions.add("" + options.solverOpts.synth.toString());
        }
        if (options.solverOpts.verif != VerifSolvers.NOT_SET) {
            // assert false : "solver opts verif need to convert old style command line args";
            backendOptions.add("-verif");
            backendOptions.add("" + options.solverOpts.verif.toString());
        }
        if (options.semOpts.arrayOobPolicy == ArrayOobPolicy.assertions) {
            backendOptions.add("--assumebcheck");
        } else if (options.semOpts.arrayOobPolicy == ArrayOobPolicy.wrsilent_rdzero) {
            // backendOptions.add("--sem-array-OOB-policy=wrsilent_rdzero");
        }
        if(options.bndOpts.inlineAmnt > 0){
            backendOptions.add("--bnd-inline-amnt");
            backendOptions.add("" + options.bndOpts.inlineAmnt);
        }
        if (options.debugOpts.showDag) {
            backendOptions.add("-showDAG");
        }
        if (options.debugOpts.outputDag != null) {
            backendOptions.add("-writeDAG");
            backendOptions.add(options.debugOpts.outputDag);
        }
        
        if (options.solverOpts.olevel >= 0) {
            backendOptions.add("--olevel");
            backendOptions.add("" + options.solverOpts.olevel);
        }
        if (options.solverOpts.simpleInputs) {
            // assert false : "need to convert old style command line args";
            backendOptions.add("-nosim");
        }
        
    }

    protected void log(String msg) {
        log(3, msg);
    }

    protected void log(int level, String msg) {
        if (options.debugOpts.verbosity >= level)
            System.out.println(msg);
    }

    public static void dump(Program prog, String message) {
        System.out.println("=============================================================");
        System.out.println("  ----- " + message + " -----");
        prog.accept(new SimpleCodePrinter());
        System.out.println("=============================================================");
    }
}
