package sketch.compiler.main.passes;

import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.cmdline.SemanticsOptions.ArrayOobPolicy;
import sketch.compiler.dataflow.simplifier.ScalarizeVectorAssignments;
import sketch.compiler.main.cmdline.SketchOptions;
import sketch.compiler.passes.lowering.*;
import sketch.compiler.passes.lowering.ProtectArrayAccesses.FailurePolicy;
import sketch.compiler.stencilSK.preprocessor.ReplaceFloatsWithBits;

public class LowerToSketch extends MetaStage {
    protected final MetaStage stencilTransform;

    public LowerToSketch(TempVarGen varGen, SketchOptions options,
            MetaStage stencilTransform)
    {
        super("lowering", "Lower for SKETCH backend", varGen, options);
        this.stencilTransform = stencilTransform;
    }

    @Override
    public Program visitProgramInner(Program prog) {

        prog = (Program) prog.accept(new AddArraySizeAssertions());
        prog = (Program) prog.accept(new ReplaceSketchesWithSpecs());
        // dump (prog, "after replskwspecs:");

        prog = (Program) prog.accept(new AddPkgNameToNames());

        prog = (Program) prog.accept(new MakeBodiesBlocks());
        // dump (prog, "MBB:");
        prog =
                (Program) prog.accept(new EliminateStructs(varGen,
                        options.bndOpts.heapSize));
        prog.debugDump("Elim Struct");
        prog = (Program) prog.accept(new DisambiguateUnaries(varGen));

        prog = stencilTransform.visitProgram(prog);

        // dump (prog, "After Stencilification.");

        prog = (Program) prog.accept(new EliminateMultiDimArrays(varGen));

        prog = (Program) prog.accept(new ExtractRightShifts(varGen));
        // dump (prog, "Extract Vectors in Casts:");
        prog = (Program) prog.accept(new ExtractVectorsInCasts(varGen));
        // dump (prog, "Extract Vectors in Casts:");
        prog = (Program) prog.accept(new SeparateInitializers());
        // dump (prog, "SeparateInitializers:");
        // prog = (Program)prog.accept(new NoRefTypes());

        prog = (Program) prog.accept(new ScalarizeVectorAssignments(varGen, true));

        prog = (Program) prog.accept(new ReplaceFloatsWithBits(varGen));
        // By default, we don't protect array accesses in SKETCH

        if (options.semOpts.arrayOobPolicy == ArrayOobPolicy.assertions)
            prog =
                    (Program) prog.accept(new ProtectArrayAccesses(
                            FailurePolicy.ASSERTION, varGen));

        prog =
                (Program) prog.accept(new EliminateNestedArrAcc(
                        options.semOpts.arrayOobPolicy == ArrayOobPolicy.assertions));
        return prog;
    }
}
