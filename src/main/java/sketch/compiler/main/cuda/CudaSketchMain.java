package sketch.compiler.main.cuda;

import sketch.compiler.ast.core.Program;
import sketch.compiler.main.seq.SequentialSketchMain;
import sketch.compiler.passes.cuda.*;
import sketch.compiler.passes.lowering.ExtractComplexLoopConditions;
import sketch.compiler.passes.lowering.SemanticChecker.ParallelCheckOption;
import sketch.compiler.passes.preprocessing.ConvertArrayAssignmentsToInout;
import sketch.compiler.passes.preprocessing.cuda.SyncthreadsCall;
import sketch.compiler.passes.preprocessing.cuda.ThreadIdReplacer;
import sketch.compiler.solvers.constructs.StaticHoleTracker;
import sketch.compiler.solvers.constructs.ValueOracle;

/**
 * cuda main functions
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
public class CudaSketchMain extends SequentialSketchMain {
    public CudaSketchMain(String[] args) {
        super(new CudaSketchOptions(args));
    }

    public class CudaBeforeSemanticCheckStage extends BeforeSemanticCheckStage {
        public CudaBeforeSemanticCheckStage() {
            super();
            addPasses(new ThreadIdReplacer(options), new InstrumentFcnCall(),
                    new SyncthreadsCall());
        }
    }

    public class CudaPreProcStage1 extends PreProcStage1 {
        public CudaPreProcStage1() {
            super();
            addPasses(new SetDefaultCudaMemoryTypes());
            addPasses(new ConvertArrayAssignmentsToInout());
            addPasses(new CopyCudaMemTypeToFcnReturn());
        }
    }

    public class CudaIRStage1 extends IRStage1 {
        public CudaIRStage1() {
            super();
            addPasses(new LowerInstrumentation(varGen, directives));
        }
    }

    public class CudaIRStage2 extends IRStage2 {
        public CudaIRStage2() {
            super();
            this.passes.add(new SplitAssignFromVarDef());
            this.passes.add(new FlattenStmtBlocks2());
            this.passes.add(new GenerateAllOrSomeThreadsFunctions(options, varGen));
            this.passes.add(new GlobalToLocalImplicitCasts(varGen, options));
        }

        @Override
        protected Program postRun(Program prog) {
            CudaSketchMain.this.debugShowPhase("threads",
                    "After transforming threads to loops", prog);

            final SemanticCheckPass semanticCheck =
                    new SemanticCheckPass(ParallelCheckOption.DONTCARE);
            ExtractComplexLoopConditions ec =
                    new ExtractComplexLoopConditions(CudaSketchMain.this.varGen);
            // final FunctionParamExtension paramExt = new FunctionParamExtension();

            prog = (Program) semanticCheck.visitProgram(prog);
            prog = (Program) ec.visitProgram(prog);
            // prog = (Program) paramExt.visitProgram(prog);

            return prog;
        }
    }

    @Override
    public BeforeSemanticCheckStage getBeforeSemanticCheckStage() {
        return new CudaBeforeSemanticCheckStage();
    }

    @Override
    public PreProcStage1 getPreProcStage1() {
        return new CudaPreProcStage1();
    }

    @Override
    public IRStage1 getIRStage1() {
        return new CudaIRStage1();
    }

    @Override
    public IRStage2 getIRStage2() {
        return new CudaIRStage2();
    }

    @Override
    public void run() {
        this.log(1, "Benchmark = " + this.benchmarkName());
        this.parseProgram();
        // this.prog =
        // (Program) (new
        // ReplaceBlockDimAndGridDim(this.options)).visitProgram(this.prog);
        this.preprocAndSemanticCheck();

        this.oracle = new ValueOracle(new StaticHoleTracker(this.varGen));
        this.partialEvalAndSolve();
        beforeUnvectorizing =
                (Program) (new DeleteInstrumentCalls()).visitProgram(beforeUnvectorizing);
        beforeUnvectorizing =
                (Program) (new DeleteCudaSyncthreads()).visitProgram(beforeUnvectorizing);
        runPrintFunctions();
        this.eliminateStar();

        this.generateCode();
        this.log(1, "[SKETCH] DONE");
    }
}
