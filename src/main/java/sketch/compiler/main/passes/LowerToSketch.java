package sketch.compiler.main.passes;

import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.cmdline.FrontendOptions.FloatEncoding;
import sketch.compiler.dataflow.simplifier.ScalarizeVectorAssignments;
import sketch.compiler.main.cmdline.SketchOptions;
import sketch.compiler.passes.lowering.*;
import sketch.compiler.passes.lowering.ProtectDangerousExprsAndShortCircuit.FailurePolicy;
import sketch.compiler.stencilSK.preprocessor.ReplaceFloatsWithBits;
import sketch.compiler.stencilSK.preprocessor.ReplaceFloatsWithFiniteField;

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
        // prog.debugDump("After aaa");
        prog = (Program) prog.accept(new ReplaceSketchesWithSpecs());
        // dump (prog, "after replskwspecs:");

        prog = (Program) prog.accept(new AddPkgNameToNames());

        prog = (Program) prog.accept(new MakeBodiesBlocks());
        // dump (prog, "MBB:");


        prog = stencilTransform.visitProgram(prog);


        prog = (Program) prog.accept(new ExtractComplexFunParams(varGen));

        prog = (Program) prog.accept(new EliminateArrayRange(varGen));


        prog = (Program) prog.accept(new EliminateMDCopies(varGen));



        prog = (Program) prog.accept(new EliminateMultiDimArrays(true, varGen));


        prog = (Program) prog.accept(new DisambiguateUnaries(varGen));

        prog =
                (Program) prog.accept(new EliminateStructs(varGen, new ExprConstInt(
                        options.bndOpts.arrSize)));

        // prog.debugDump("After ES");


        // dump (prog, "After Stencilification.");


        prog = (Program) prog.accept(new ExtractRightShifts(varGen));

        // dump (prog, "Extract Vectors in Casts:");
        prog = (Program) prog.accept(new ExtractVectorsInCasts(varGen));
        // dump (prog, "Extract Vectors in Casts:");
        prog = (Program) prog.accept(new SeparateInitializers());
        // dump (prog, "SeparateInitializers:");
        // prog = (Program)prog.accept(new NoRefTypes());
        // prog.debugDump("Before SVA");

        if (options.feOpts.fencoding == FloatEncoding.AS_BIT) {
            prog = (Program) prog.accept(new ReplaceFloatsWithBits(varGen));
        } else if (options.feOpts.fencoding == FloatEncoding.AS_FFIELD) {
            prog = (Program) prog.accept(new ReplaceFloatsWithFiniteField(varGen));
        }


        prog =
                (Program) prog.accept(new ProtectDangerousExprsAndShortCircuit(
                        FailurePolicy.ASSERTION, varGen));


        prog = (Program) prog.accept(new ScalarizeVectorAssignments(varGen, true));

        // prog.debugDump("After SVA");

        prog = (Program) prog.accept(new EliminateNestedArrAcc(false));
        return prog;
    }
}
