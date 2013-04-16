package sketch.compiler.main.passes;

import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.cmdline.FrontendOptions.FloatEncoding;
import sketch.compiler.dataflow.simplifier.ScalarizeVectorAssignments;
import sketch.compiler.main.cmdline.SketchOptions;
import sketch.compiler.passes.lowering.*;
import sketch.compiler.passes.lowering.ProtectDangerousExprsAndShortCircuit.FailurePolicy;
import sketch.compiler.passes.printers.SimpleCodePrinter;
import sketch.compiler.passes.spmd.GlobalToLocalCasts;
import sketch.compiler.passes.spmd.ReplaceParamExprArrayRange;
import sketch.compiler.passes.spmd.SpmdTransform;
import sketch.compiler.stencilSK.preprocessor.ReplaceFloatsWithBits;
import sketch.compiler.stencilSK.preprocessor.ReplaceFloatsWithFiniteField;
import sketch.compiler.stencilSK.preprocessor.ReplaceFloatsWithFixpoint;

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
        SimpleCodePrinter prt = new SimpleCodePrinter();
        // System.out.println("before aasa:");
        // prog.accept(prt);
        prog = (Program) prog.accept(new AddArraySizeAssertions());

        // FIXME xzl: use efs instead of es, can generate wrong program!
        // System.out.println("before efs:");
        // prog.accept(prt);

        if (options.feOpts.elimFinalStructs) {
            prog =
                    (Program) prog.accept(new EliminateFinalStructs(varGen,
                            options.bndOpts.arr1dSize));
            System.out.println("after efs:");
            prog.accept(prt);
        }

        prog = (Program) prog.accept(new MakeMultiDimExplicit(varGen));
        // System.out.println("after mmde:");
        // prog.accept(prt);

        prog = (Program) prog.accept(new ReplaceSketchesWithSpecs());
        // dump (prog, "after replskwspecs:");

        prog = (Program) prog.accept(new AddPkgNameToNames());

        prog = (Program) prog.accept(new MakeBodiesBlocks());
        // dump (prog, "MBB:");

        prog = stencilTransform.visitProgram(prog);

        prog = (Program) prog.accept(new ExtractComplexFunParams(varGen));

        // prog.accept(prt);

        
        prog = (Program) prog.accept(new SeparateInitializers());
        prog = (Program) prog.accept(new FlattenStmtBlocks());
        SpmdTransform tf = new SpmdTransform(options, varGen);
        prog = (Program) prog.accept(tf);
        prog = (Program) prog.accept(new GlobalToLocalCasts(varGen, tf));
        
        prog = (Program) prog.accept(new ReplaceParamExprArrayRange(varGen));
        
        
        prog = (Program) prog.accept(new EliminateArrayRange(varGen));


        prog = (Program) prog.accept(new EliminateMDCopies(varGen));



        prog =
                (Program) prog.accept(new ProtectDangerousExprsAndShortCircuit(
                        FailurePolicy.ASSERTION, varGen));
                        


                        
        System.out.println("after rpear:");
        prog.accept(prt);
        // System.out.println("after ear:");
        // prog.accept(prt);
        // prog.accept(prt);

        prog.debugDump("After Protect");

        prog = (Program) prog.accept(new EliminateMultiDimArrays(false, varGen));


        prog = (Program) prog.accept(new DisambiguateUnaries(varGen));


        // TODO xzl: temporarily remove EliminateStructs

        

        prog =
                (Program) prog.accept(new EliminateStructs(varGen, new ExprConstInt(
                        options.bndOpts.arrSize)));

        // dump (prog, "After Stencilification.");


        prog = (Program) prog.accept(new ExtractRightShifts(varGen));


        // dump (prog, "Extract Vectors in Casts:");
        prog = (Program) prog.accept(new ExtractVectorsInCasts(varGen));



        prog = (Program) prog.accept(new SeparateInitializers());


        // FIXME xzl: all replacing does not consider += -= etc.
        if (options.feOpts.fpencoding == FloatEncoding.AS_BIT) {
            prog = (Program) prog.accept(new ReplaceFloatsWithBits(varGen));
        } else if (options.feOpts.fpencoding == FloatEncoding.AS_FFIELD) {
            prog = (Program) prog.accept(new ReplaceFloatsWithFiniteField(varGen));
        } else if (options.feOpts.fpencoding == FloatEncoding.AS_FIXPOINT) {
            prog = (Program) prog.accept(new ReplaceFloatsWithFixpoint(varGen));
        }

        prog = (Program) prog.accept(new LoopInvariantAssertionHoisting());


        prog = (Program) prog.accept(new ScalarizeVectorAssignments(varGen, false));


        prog = (Program) prog.accept(new EliminateNestedArrAcc(false));
        

        if (options.feOpts.truncVarArr) {
            prog = (Program) prog.accept(new TruncateVarArray(options, varGen));
        }
        return prog;
    }
}
