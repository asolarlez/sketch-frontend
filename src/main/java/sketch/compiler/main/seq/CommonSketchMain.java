package sketch.compiler.main.seq;

import java.util.Set;
import java.util.Vector;

import sketch.compiler.Directive;
import sketch.compiler.Directive.OptionsDirective;
import sketch.compiler.ast.core.Program;
import sketch.compiler.cmdline.SemanticsOptions.ArrayOobPolicy;
import sketch.compiler.cmdline.SolverOptions.SynthSolvers;
import sketch.compiler.cmdline.SolverOptions.VerifSolvers;
import sketch.compiler.passes.printers.SimpleCodePrinter;

public class CommonSketchMain {
    public SequentialSketchOptions options;

    public CommonSketchMain(SequentialSketchOptions options) {
        this.options = options;
    }

    public static void dump(Program prog) {
        dump(prog, "");
    }

    public boolean showPhaseOpt(String opt) {
        return (options.debugOpts.showPhase != null) &&
                options.debugOpts.showPhase.contains(opt);
    }
    
    public void debugShowPhase(String opt, String desc, Program prog) {
        if (showPhaseOpt(opt)) {
            prog.debugDump(desc);
        }
    }

    protected void backendParameters() {
        options.backendOptions = new Vector<String>();
        Vector<String> backendOptions = options.backendOptions;

        // pass all short-style arguments to the backend
        backendOptions.addAll(options.backendArgs);

        backendOptions.add("-overrideInputs");
        backendOptions.add("" + options.bndOpts.inbits);

        backendOptions.add("-verbosity");
        backendOptions.add("" + options.debugOpts.verbosity);

        if (options.solverOpts.seed != 0) {
            backendOptions.add("-seed");
            backendOptions.add("" + options.solverOpts.seed);
        }
        if (options.debugOpts.cex) {
            backendOptions.add("-showinputs");
        }
        if (options.solverOpts.synth != SynthSolvers.NOT_SET) {
            backendOptions.add("-synth");
            backendOptions.add("" + options.solverOpts.synth.toString());
        }
        if (options.solverOpts.verif != VerifSolvers.NOT_SET) {
            backendOptions.add("-verif");
            backendOptions.add("" + options.solverOpts.verif.toString());
        }
        if (options.semOpts.arrayOobPolicy == ArrayOobPolicy.assertions) {
            backendOptions.add("-assumebcheck");
        }
        if(options.solverOpts.inline > 0){
            backendOptions.add("-inlineamnt");
            backendOptions.add("" + options.solverOpts.inline);
        }
        
        if (options.solverOpts.olevel >= 0) {
            backendOptions.add("-olevel");
            backendOptions.add("" + options.solverOpts.olevel);
        }
        if (options.solverOpts.simpleInputs) {
            backendOptions.add("-nosim");
        }
        
    }

    protected void processDirectives(Set<Directive> D) {
        for (Directive d : D)
            if (d instanceof OptionsDirective)
                options.prependArgsAndReparse(((OptionsDirective) d).options(), false);
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