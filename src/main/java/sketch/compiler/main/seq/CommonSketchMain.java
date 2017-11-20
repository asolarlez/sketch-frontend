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
        if (options.bndOpts.angelicbits > 0) {
            backendOptions.add("--bnd-angelicbits");
            backendOptions.add("" + options.bndOpts.angelicbits);
        }
        if (options.bndOpts.angelicArrsz > 0) {
            backendOptions.add("--bnd-angelic-arrsz");
            backendOptions.add("" + options.bndOpts.angelicArrsz);
        }

        backendOptions.add("--boundmode");
        backendOptions.add("" + options.bndOpts.boundMode);

        backendOptions.add("--verbosity");
        backendOptions.add(""+ options.debugOpts.verbosity);
        backendOptions.add("--print-version"); // run by default

        if (options.solverOpts.seed != 0) {
            // parallel running will use its own seeds systematically
            if (!options.solverOpts.parallel) {
                backendOptions.add("--seed");
                backendOptions.add("" + options.solverOpts.seed);
            }
        }
        if (options.solverOpts.simiters != 0) {
            backendOptions.add("-simiters");
            backendOptions.add("" + options.solverOpts.simiters);
        }
        if (options.solverOpts.randassign) {
            backendOptions.add("-randassign");
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

        if (options.bndOpts.intRange > 0) {
            backendOptions.add("--bndwrand");
            backendOptions.add("" + options.bndOpts.intRange);
        }

        if (options.solverOpts.lightverif) {
            backendOptions.add("-lightverif");
        }

        if (options.debugOpts.showDag) {
            backendOptions.add("-showDAG");
        }

        if (options.debugOpts.outputDag != null) {
            backendOptions.add("-writeDAG");
            backendOptions.add(options.debugOpts.outputDag);
        }

        if (options.bndOpts.dagSize > 0) {
            backendOptions.add("--bnd-dag-size");
            backendOptions.add("" + options.bndOpts.dagSize);
        }

        if (options.solverOpts.olevel >= 0) {
            backendOptions.add("--olevel");
            backendOptions.add("" + options.solverOpts.olevel);
        }
        if (options.solverOpts.simpleInputs) {
            // assert false : "need to convert old style command line args";
            backendOptions.add("-nosim");
        }
        if (options.bndOpts.angTupleDepth > 0) {
            backendOptions.add("-angelictupledepth");
            backendOptions.add("" + options.bndOpts.angTupleDepth);
        }
        if (options.bndOpts.srcTupleDepth > 0) {
            backendOptions.add("-srctupledepth");
            backendOptions.add("" + options.bndOpts.srcTupleDepth);
        }
        if (options.solverOpts.onlySpRand) {
            backendOptions.add("-onlysprandassign");
        }
        if (options.solverOpts.spRandBias > 0) {
            backendOptions.add("-sprandbias");
            backendOptions.add("" + options.solverOpts.spRandBias);
        }

		backendOptions.add("-numericalsolvermode");
		backendOptions.add(options.solverOpts.numSolverMode);

		if (options.solverOpts.useSnopt) {
			backendOptions.add("-usesnopt");
		}

		if (options.solverOpts.useEager) {
			backendOptions.add("-useeager");
		}

		if (options.solverOpts.relaxBoolHoles) {
			backendOptions.add("-relaxboolholes");
		}

		backendOptions.add("-numtries");
		backendOptions.add("" + options.solverOpts.numTries);

		if (options.solverOpts.disableSatSug) {
			backendOptions.add("-disablesatsuggestions");
		}

		if (options.solverOpts.disableUnsatSug) {
			backendOptions.add("-disableunsatsuggestions");
		}

		backendOptions.add("-conflictcutoff");
		backendOptions.add("" + options.solverOpts.conflictCutoff);

		backendOptions.add("-maxrestarts");
		backendOptions.add("" + options.solverOpts.maxRestarts);

		backendOptions.add("-costoption");
		backendOptions.add("" + options.solverOpts.costOption);

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
