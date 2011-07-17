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
import sketch.compiler.passes.cleanup.RemoveDumbArrays;
import sketch.compiler.passes.lowering.AssembleInitializers;
import sketch.compiler.passes.preprocessing.RemoveShallowTempVars;
import sketch.compiler.passes.printers.SimpleCodePrinter;

/**
 * Substitute a solution into a program, and simplify the program.
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
public class CleanupFinalCode extends MetaStage {
    protected final RecursionControl rctrl;

    public CleanupFinalCode(TempVarGen varGen, SketchOptions options,
            RecursionControl rctrl)
    {
        super("cleanup", "Clean up sketch after substitutions for readability", varGen,
                options);
        this.rctrl = rctrl;
    }

    @Override
    public Program visitProgramInner(Program prog) {
        // dataflow pass
        final PreprocessSketch preproc =
                new PreprocessSketch(varGen, options.bndOpts.unrollAmnt, rctrl, true);
        prog = (Program) prog.accept(preproc);
        prog = (Program) prog.accept(new FlattenStmtBlocks());
        prog = (Program) prog.accept(new MakeCastsExplicit());
        prog = (Program) prog.accept(new EliminateTransAssns());
        prog = (Program) prog.accept(new EliminateDeadCode(options.feOpts.keepAsserts));
        prog = (Program) prog.accept(new RemoveDumbArrays());
        prog.accept(new SimpleCodePrinter());
        prog = (Program) prog.accept(new EliminateTransAssns());
        prog = (Program) prog.accept(new EliminateDeadCode(options.feOpts.keepAsserts));
        prog = (Program) prog.accept(new SimplifyVarNames());
        prog = (Program) prog.accept(new AssembleInitializers());
        prog = (Program) prog.accept(new RemoveShallowTempVars());
        prog = (Program) prog.accept(new AssembleInitializers());

        return prog;
    }
}
