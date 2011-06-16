package sketch.compiler.main.passes;

import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.dataflow.deadCodeElimination.EliminateDeadCode;
import sketch.compiler.dataflow.eliminateTransAssign.EliminateTransAssns;
import sketch.compiler.dataflow.preprocessor.FlattenStmtBlocks;
import sketch.compiler.dataflow.preprocessor.PreprocessSketch;
import sketch.compiler.dataflow.preprocessor.SimplifyVarNames;
import sketch.compiler.dataflow.recursionCtrl.RecursionControl;
import sketch.compiler.main.cmdline.SketchOptions;
import sketch.compiler.passes.cleanup.MakeCastsExplicit;
import sketch.compiler.passes.lowering.AssembleInitializers;
import sketch.compiler.solvers.constructs.ValueOracle;
import sketch.compiler.passes.preprocessing.RemoveShallowTempVars;
import sketch.compiler.stencilSK.EliminateStarStatic;

/**
 * Substitute a solution into a program, and simplify the program.
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
public class SubstituteSolution extends MetaStage {
    protected final ValueOracle solution;
    protected final RecursionControl rctrl;

    public SubstituteSolution(TempVarGen varGen, SketchOptions options,
            ValueOracle solution, RecursionControl rctrl)
    {
        super(varGen, options);
        this.solution = solution;
        this.rctrl = rctrl;
    }

    @Override
    public Program visitProgramInner(Program prog) {
        EliminateStarStatic eliminate_star = new EliminateStarStatic(solution);
        Program p = (Program) prog.accept(eliminate_star);

        if (options.feOpts.outputXml != null) {
            eliminate_star.dump_xml(options.feOpts.outputXml);
        }

        // dataflow pass
        final PreprocessSketch preproc =
                new PreprocessSketch(varGen, options.bndOpts.unrollAmnt, rctrl, true);
        p = (Program) p.accept(preproc);
        // dump(finalCode, "After partially evaluating generated code.");
        p = (Program) p.accept(new FlattenStmtBlocks());
        // dump(finalCode, "After Flattening.");
        p = (Program) p.accept(new MakeCastsExplicit());
        p = (Program) p.accept(new EliminateTransAssns());
        // System.out.println("=========  After ElimTransAssign  =========");

        p = (Program) p.accept(new EliminateDeadCode(options.feOpts.keepAsserts));
        // dump(finalCode, "After Dead Code elimination.");
        // System.out.println("=========  After ElimDeadCode  =========");
        p = (Program) p.accept(new SimplifyVarNames());
        p = (Program) p.accept(new AssembleInitializers());
        p = (Program)p.accept(new RemoveShallowTempVars());
        p = (Program)p.accept(new AssembleInitializers());

        // TODO: integrate these?
        // if( false && params.hasFlag("outputcode") ) {
        // prog=(Program) prog.accept(new AssembleInitializers());
        // prog=(Program) prog.accept(new BitVectorPreprocessor(varGen));
        // //prog.accept(new SimpleCodePrinter());
        // prog=(Program) prog.accept(new BitTypeRemover(varGen));
        // prog=(Program) prog.accept(new SimplifyExpressions());
        // }

        return p;
    }
}
