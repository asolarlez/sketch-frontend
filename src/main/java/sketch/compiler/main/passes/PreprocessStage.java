package sketch.compiler.main.passes;

import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.cmdline.SolverOptions.ReorderEncoding;
import sketch.compiler.dataflow.cflowChecks.PerformFlowChecks;
import sketch.compiler.dataflow.preprocessor.PreprocessSketch;
import sketch.compiler.dataflow.preprocessor.TypeInferenceForStars;
import sketch.compiler.dataflow.recursionCtrl.RecursionControl;
import sketch.compiler.main.cmdline.SketchOptions;
import sketch.compiler.passes.lowering.*;
import sketch.compiler.passes.optimization.ReplaceMinLoops;
import sketch.compiler.passes.preprocessing.EliminateFieldHoles;
import sketch.compiler.passes.preprocessing.EliminateListOfFieldsMacro;
import sketch.compiler.passes.preprocessing.ExpandRepeatCases;
import sketch.compiler.passes.preprocessing.MainMethodCreateNospec;
import sketch.compiler.passes.preprocessing.RemoveADTHoles;
import sketch.compiler.passes.preprocessing.TypeInferenceForADTHoles;
import sketch.compiler.passes.types.CheckProperFinality;

/**
 * @author Armando Solar-Lezama
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
public class PreprocessStage extends MetaStage {
    // protected final CompilerStage preproc1;
    // protected final CompilerStage ir1;
    protected final RecursionControl rctrl;
    protected final boolean partialEval;

    public PreprocessStage(TempVarGen varGen, SketchOptions options,
    /* CompilerStage preproc1, CompilerStage ir1, */RecursionControl rctrl,
            boolean partialEval)
    {
        super("preproc", "Preprocessing (used for all further transformations)", varGen,
                options);
        // this.preproc1 = preproc1;
        // this.ir1 = ir1;
        this.rctrl = rctrl;
        this.partialEval = partialEval;
    }

    @Override
    public Program visitProgramInner(Program prog) {

		// prog.debugDump("************************************** Inside visit program inner");

        boolean useInsertEncoding =
                (options.solverOpts.reorderEncoding == ReorderEncoding.exponential);


        prog = (Program) prog.accept(new SeparateInitializers());

		// prog.debugDump("************************************** Before BlockifyRewriteableStmts");
        prog = (Program) prog.accept(new BlockifyRewriteableStmts());

		// prog.debugDump("************************************** After BlockifyRewriteableStmts");
        prog = (Program) prog.accept(new ReplaceMinLoops(varGen));

		// prog.debugDump("After Replace Min Loops");

        // FIXME xzl: temporarily disable ExtractComplexLoopCondition to help stencil
		// prog = (Program) prog.accept(new
		// ExtractComplexLoopConditions(varGen));
        // prog.debugDump("before regens");


		// prog.debugDump("************************************** After extract
		// complex loop");

        prog = (Program) prog.accept(new EliminateBitSelector(varGen));

		// prog.debugDump("************************************** 1");

        prog.accept(new CheckProperFinality());

		// prog.debugDump("************************************** 2");

        prog = (Program) prog.accept(new MainMethodCreateNospec());

		// prog.debugDump("************************************** 3");

        // prog = preproc1.run(prog);

        // prog = (Program)prog.accept (new BoundUnboundedLoops (varGen, params.flagValue
        // ("unrollamnt")));
        // prog = (Program)prog.accept(new NoRefTypes());
        prog =
                (Program) prog.accept(new EliminateReorderBlocks(varGen,
                        useInsertEncoding));

		// prog.debugDump("************************************** 4");

        prog = (Program) prog.accept(new EliminateInsertBlocks(varGen));

		// prog.debugDump("************************************** 5");

        prog = (Program) prog.accept(new DisambiguateUnaries(varGen));
        
        // prog.debugDump("After remove expr get");

        // Remove ExprGet will generate regens and adt holes
        prog = (Program) prog.accept(new EliminateRegens(varGen));
		// prog.debugDump("************************************** 7");

        prog = (Program) prog.accept(new FunctionParamExtension(true, varGen));
        // prog.debugDump();

		// prog.debugDump("************************************** 8");


		prog = (Program) prog.accept(new GlobalsToParams(varGen));

		// prog.debugDump("************************************** 9");

        prog = (Program) prog.accept(new TypeInferenceForADTHoles());
        // prog = ir1.run(prog);
//        prog.debugDump("************************************** Before type inference");
		// prog.debugDump("before type inference");

        prog = (Program) prog.accept(new TypeInferenceForStars());
//		prog.debugDump("************************************** Before Local Variable replacer");
//		prog = (Program) prog.accept(new LocalVariablesReplacer(varGen));
//		prog.debugDump("************************************** After Local Variable replacer");
        
        //prog.debugDump("af");
        

        if (!SketchOptions.getSingleton().feOpts.lowOverhead) {
            prog.accept(new PerformFlowChecks());
        }

        prog = (Program) prog.accept(new EliminateUnboxedStructs(varGen));



        prog = (Program) prog.accept(new EliminateNestedArrAcc(true));


        prog = (Program) prog.accept(new MakeMultiDimExplicit(varGen));
        if (partialEval) {
            prog =
                    (Program) prog.accept(new PreprocessSketch(varGen,
                            options.bndOpts.unrollAmnt, rctrl));
        }
        prog = (Program) prog.accept(new ExpandRepeatCases());
        // prog.debugDump();
        prog = (Program) prog.accept(new EliminateListOfFieldsMacro());
        // prog.debugDump("af");

        // TODO: TypeInferenceForADTHoles should deal with function parameters

        prog = (Program) prog.accept(new TypeInferenceForADTHoles());

        prog =
                (Program) prog.accept(new RemoveADTHoles(varGen, options.bndOpts.arrSize,
                        options.bndOpts.gucDepth));
        // prog.debugDump();
        prog = (Program) prog.accept(new EliminateRegens(varGen));
        prog = (Program) prog.accept(new TypeInferenceForADTHoles());
        prog = (Program) prog.accept(new TypeInferenceForStars());
        prog = (Program) prog.accept(new EliminateFieldHoles());
        return prog;
    }
}
