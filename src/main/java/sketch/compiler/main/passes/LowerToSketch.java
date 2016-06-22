package sketch.compiler.main.passes;

import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.dataflow.simplifier.ScalarizeVectorAssignments;
import sketch.compiler.main.cmdline.SketchOptions;
import sketch.compiler.passes.lowering.*;
import sketch.compiler.passes.lowering.ProtectDangerousExprsAndShortCircuit.FailurePolicy;
import sketch.compiler.passes.preprocessing.CombineFunctionCalls;
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



        // ADT
        prog = (Program) prog.accept(new MergeADT());
        // prog.debugDump("afterMergeADT");
        prog = (Program) prog.accept(new EliminateGenerics());

        // prog.debugDump("after Generics");

        if (false) {
            prog = (Program) prog.accept(new CombineFunctionCalls(varGen));
        }

        if (true) {
            prog = (Program) prog.accept(new CollectFunCallsToCombine());
        }

        prog = (Program) prog.accept(new AddArraySizeAssertions());
        // prog.debugDump("aa");

        // FIXME xzl: use efs instead of es, can generate wrong program!
        // System.out.println("before efs:");
        // prog.accept(prt);

        if (options.feOpts.elimFinalStructs) {
            prog =
                    (Program) prog.accept(new EliminateFinalStructs(varGen,
                            options.bndOpts.arr1dSize));
        }

        prog = (Program) prog.accept(new ReplaceSketchesWithSpecs());

        // prog.debugDump("Before reinterpret");
        prog = (Program) prog.accept(new ReinterpretAssumes());
        // prog.debugDump("After reinterpret");

        prog = (Program) prog.accept(new AddPkgNameToNames());

        prog = (Program) prog.accept(new MakeBodiesBlocks());


        if (!SketchOptions.getSingleton().feOpts.lowOverhead) {
            prog = stencilTransform.visitProgram(prog);
        }


        prog = (Program) prog.accept(new ExtractComplexFunParams(varGen));
        
        prog = (Program) prog.accept(new SeparateInitializers());
        
        prog = (Program) prog.accept(new FlattenStmtBlocks());
        

        if (false) { // temporarily disabled in the main branch.
            SpmdTransform tf = new SpmdTransform(options, varGen);
            prog = (Program) prog.accept(tf);
            prog = (Program) prog.accept(new GlobalToLocalCasts(varGen, tf));
        }
        prog = (Program) prog.accept(new ReplaceParamExprArrayRange(varGen));
        
        
        prog = (Program) prog.accept(new EliminateArrayRange(varGen));


        prog = (Program) prog.accept(new EliminateMDCopies(varGen));



        prog =
                (Program) prog.accept(new ProtectDangerousExprsAndShortCircuit(
                        FailurePolicy.ASSERTION, varGen));
                        


                        

        prog = (Program) prog.accept(new EliminateMultiDimArrays(false, varGen));


        prog = (Program) prog.accept(new DisambiguateUnaries(varGen));
        // pass to eliminate immutable structs
        // prog.debugDump("Before EIS");
        prog = (Program) prog.accept(new EliminateImmutableStructs());

        // prog.debugDump("After EIS");
        prog =
                (Program) prog.accept(new EliminateStructs(varGen, new ExprConstInt(
                        options.bndOpts.arrSize)));

        // dump(prog, "After Stencilification.");
        // prog.debugDump("After es");

        prog = (Program) prog.accept(new EliminateNestedTuples(varGen));
        prog =
                (Program) prog.accept(new EliminateNestedTupleReads(varGen,
                        new ExprConstInt(options.bndOpts.arrSize)));
        prog =
                (Program) prog.accept(new ExtractRightShifts(varGen));
        // prog.debugDump();
        // dump (prog, "Extract Vectors in Casts:");
        prog = (Program) prog.accept(new ExtractVectorsInCasts(varGen));



        prog = (Program) prog.accept(new SeparateInitializers());


        
        switch(options.feOpts.fpencoding){
        case AS_BIT:
            prog = (Program) prog.accept(new ReplaceFloatsWithBits(varGen));
            break;
        case AS_FFIELD:
            prog = (Program) prog.accept(new ReplaceFloatsWithFiniteField(varGen));
            break;
        case AS_FIXPOINT:
            prog = (Program) prog.accept(new ReplaceFloatsWithFixpoint(varGen));
            break;
        case TO_BACKEND:
            
        }
        
        


        prog = (Program) prog.accept(new LoopInvariantAssertionHoisting());

        prog = (Program) prog.accept(new ScalarizeVectorAssignments(varGen, false));


        prog = (Program) prog.accept(new EliminateNestedArrAcc(false));

        if (false) {
        prog =
                (Program) prog.accept(new MakeLoopsRecursive(varGen,
                        options.bndOpts.unrollAmnt));
        prog.debugDump("After making loops recursive");
        }

        prog = (Program) prog.accept(new EliminateHugeArrays());

        if (options.feOpts.truncVarArr) {
            prog = (Program) prog.accept(new TruncateVarArray(options, varGen));
        }
        // prog.debugDump("aa");

        return prog;
    }
}
