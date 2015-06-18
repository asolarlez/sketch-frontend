/*
 * Copyright 2003 by the Massachusetts Institute of Technology.
 *
 * Permission to use, copy, modify, and distribute this
 * software and its documentation for any purpose and without
 * fee is hereby granted, provided that the above copyright
 * notice appear in all copies and that both that copyright
 * notice and this permission notice appear in supporting
 * documentation, and that the name of M.I.T. not be used in
 * advertising or publicity pertaining to distribution of the
 * software without specific, written prior permission.
 * M.I.T. makes no representations about the suitability of
 * this software for any purpose.  It is provided "as is"
 * without express or implied warranty.
 */

package sketch.compiler.main.seq;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Vector;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.FEVisitor;
import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprStar;
import sketch.compiler.codegenerators.OutputHoleFunc;
import sketch.compiler.dataflow.recursionCtrl.AdvancedRControl;
import sketch.compiler.dataflow.recursionCtrl.DelayedInlineRControl;
import sketch.compiler.dataflow.recursionCtrl.RecursionControl;
import sketch.compiler.main.PlatformLocalization;
import sketch.compiler.main.cmdline.SketchOptions;
import sketch.compiler.main.other.ErrorHandling;
import sketch.compiler.main.passes.CleanupFinalCode;
import sketch.compiler.main.passes.LowerToSketch;
import sketch.compiler.main.passes.OutputCCode;
import sketch.compiler.main.passes.ParseProgramStage;
import sketch.compiler.main.passes.PreprocessStage;
import sketch.compiler.main.passes.StencilTransforms;
import sketch.compiler.main.passes.SubstituteSolution;
import sketch.compiler.passes.annotations.CompilerPassDeps;
import sketch.compiler.passes.cleanup.EliminateAliasesInRefParams;
import sketch.compiler.passes.cuda.CopyCudaMemTypeToFcnReturn;
import sketch.compiler.passes.cuda.FlattenStmtBlocks2;
import sketch.compiler.passes.cuda.LowerInstrumentation;
import sketch.compiler.passes.cuda.SetDefaultCudaMemoryTypes;
import sketch.compiler.passes.cuda.SplitAssignFromVarDef;
import sketch.compiler.passes.lowering.ConstantReplacer;
import sketch.compiler.passes.lowering.EliminateComplexForLoops;
import sketch.compiler.passes.lowering.EliminateMultiDimArrays;
import sketch.compiler.passes.lowering.EliminateStructs;
import sketch.compiler.passes.lowering.ExtractComplexLoopConditions;
import sketch.compiler.passes.lowering.ReplaceImplicitVarDecl;
import sketch.compiler.passes.lowering.SemanticChecker;
import sketch.compiler.passes.lowering.SemanticChecker.ParallelCheckOption;
import sketch.compiler.passes.preprocessing.ConvertArrayAssignmentsToInout;
import sketch.compiler.passes.preprocessing.DisambiguateCallsAndTypeCheck;
import sketch.compiler.passes.preprocessing.EliminateMacros;
import sketch.compiler.passes.preprocessing.EliminateTripleEquals;
import sketch.compiler.passes.preprocessing.ExpandRepeatCases;
import sketch.compiler.passes.preprocessing.MethodRename;
import sketch.compiler.passes.preprocessing.MinimizeFcnCall;
import sketch.compiler.passes.preprocessing.RemoveFunctionParameters;
import sketch.compiler.passes.preprocessing.SetDeterministicFcns;
import sketch.compiler.passes.preprocessing.spmd.PidReplacer;
import sketch.compiler.passes.preprocessing.spmd.SpmdbarrierCall;
import sketch.compiler.solvers.SATBackend;
import sketch.compiler.solvers.SolutionStatistics;
import sketch.compiler.solvers.constructs.ValueOracle;
import sketch.compiler.solvers.parallel.StrategicalBackend;
import sketch.util.ControlFlowException;
import sketch.util.exceptions.InternalSketchException;
import sketch.util.exceptions.ProgramParseException;
import sketch.util.exceptions.SketchException;

import static sketch.util.DebugOut.printError;

import static sketch.util.Misc.nonnull;

/**
 * Convert StreamIt programs to legal Java code.  This is the main
 * entry point for the StreamIt syntax converter.  Running it as
 * a standalone program reads the list of files provided on the
 * command line and produces equivalent Java code on standard
 * output or the file named in the <tt>--output</tt> command-line
 * parameter.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id$
 */
public class SequentialSketchMain extends CommonSketchMain
{
    public SequentialSketchMain(String[] args) {
        super(new SketchOptions(args));
    }

	/** for subclasses */
    public SequentialSketchMain(SketchOptions options) {
        super(options);
    }

    public boolean isParallel () {
		return false;
	}

    public RecursionControl visibleRControl (Program p) {
		// return new BaseRControl(params.inlineAmt);
        return new AdvancedRControl(options.bndOpts.branchAmnt,
                options.bndOpts.inlineAmnt, true, p);
	}

	/**
	 * This function produces a recursion control that is used by all transformations that are not user visible.
	 * In particular, the conversion to boolean. By default it is the same as the visibleRControl.
	 * @return
	 */
	public RecursionControl internalRControl(){

	    //return new AdvancedRControl(options.bndOpts.branchAmnt, options.bndOpts.inlineAmnt, prog);
		return new DelayedInlineRControl(0, 0);
	}


	protected TempVarGen varGen = new TempVarGen();

	/** hack to check deps across stages; accessed by CompilerStage */
    public final HashSet<Class<? extends FEVisitor>> runClasses =
            new HashSet<Class<? extends FEVisitor>>();

    /**
     * Things that are part of the language, but not part of the syntax since they're just
     * function calls.
     * 
     * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
     * @license This file is licensed under BSD license, available at
     *          http://creativecommons.org/licenses/BSD/. While not required, if you make
     *          changes, please consider contributing back!
     */
    public class BeforeSemanticCheckStage extends CompilerStage {
        public BeforeSemanticCheckStage() {
            super(SequentialSketchMain.this);
            FEVisitor[] passes2 =
 { new MinimizeFcnCall() /* new TprintFcnCall(), */
            };
            passes = new Vector<FEVisitor>(Arrays.asList(passes2));
        }
    }

    public class LowerSyntaxSugar extends CompilerStage {
        public LowerSyntaxSugar() {
            super(SequentialSketchMain.this);
            FEVisitor[] passes2 =
                    {
                            /*
                             * new ReplaceMinLoops(varGen), new MainMethodCreateNospec(),
                             */
                    new SetDeterministicFcns()/*
                                               * , new
                                               * ReplaceParforLoops(options.cudaOpts.
                                               * threadBlockDim, varGen)
                                               */, new ReplaceImplicitVarDecl(),
                            new SetDefaultCudaMemoryTypes(),
                            new ConvertArrayAssignmentsToInout(),
                            new CopyCudaMemTypeToFcnReturn() };
            passes = new Vector<FEVisitor>(Arrays.asList(passes2));
        }
    }

    public class LowerHighLevelConstructs extends CompilerStage {
        public LowerHighLevelConstructs() {
            super(SequentialSketchMain.this);
            FEVisitor[] passes2 =
 { // new GlobalsToParams(varGen),
                    new LowerInstrumentation(varGen)
                    // , new FlattenCommaMultidimArrays(null)
                    };
            passes = new Vector<FEVisitor>(Arrays.asList(passes2));
        }
    }

    /**
     * The intermediate stage that generates low-level C code, i.e. with the SPMD model
     * sequentialized
     * 
     * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
     */
    public class LowLevelCStage extends CompilerStage {
        public LowLevelCStage() {
            super(SequentialSketchMain.this);
            FEVisitor[] passes2 = {};
            passes = new Vector<FEVisitor>(Arrays.asList(passes2));
        }
    }

    public class SpmdLowLevelCStage extends LowLevelCStage {

        public SpmdLowLevelCStage() {
            super();
            // this.passes.add(new FlattenStmtBlocks2());
            // this.passes.add(new SplitAssignFromVarDef());
            this.passes.add(new SplitAssignFromVarDef());
            this.passes.add(new FlattenStmtBlocks2());
            this.passes.add(new EliminateComplexForLoops(varGen));
            // TODO: should not add tf here
            // should do this after LowerToSketch
            // there, all structs are eliminated
            // and bulk array operations are turned to loops
            // array bounds checking are performed
            // we want to add [SpmdMaxNProc] to be the inner most (least significant) dimension
            // SpmdTransform tf = new SpmdTransform(options, varGen);
            // this.passes.add(tf);
            // this.passes.add(new GlobalToLocalCasts(varGen, tf));
            // this.passes.add(new ReplaceParamExprArrayRange(varGen));
        }

        @Override
        protected Program postRun(Program prog) {
            final SemanticCheckPass semanticCheck =
                    new SemanticCheckPass(ParallelCheckOption.DONTCARE, false);
            // FIXME xzl: temporarily disable extractComplexLoopCond to help stencil
            ExtractComplexLoopConditions ec =
                    new ExtractComplexLoopConditions(SequentialSketchMain.this.varGen);
            // final FunctionParamExtension paramExt = new FunctionParamExtension();

            prog = (Program) semanticCheck.visitProgram(prog);
            // FIXME xzl: temporarily disable extractComplexLoopCond to help stencil
            prog = (Program) ec.visitProgram(prog);
            // prog = (Program) paramExt.visitProgram(prog);
            return prog;
        }
    }




    // [start] function overloads for stage classes
    public BeforeSemanticCheckStage getBeforeSemanticCheckStage() {
        return new BeforeSemanticCheckStage();
    }

    public LowerSyntaxSugar getPreProcStage1() {
        return new LowerSyntaxSugar();
    }



    public LowLevelCStage getIRStage2_LLC(Program prog) {
        /*
        if (new ContainsStencilFunction().run(prog)) {
            if ((new ContainsCudaCode()).run(prog)) {
                final UnsupportedSketchException exception =
                        new UnsupportedSketchException(
                                "Program contains both CUDA and stencil code");
                exception.setLastGoodProgram(new LastGoodProgram(
                        "CudaSketchMain/getIRStage2_LLC()", prog));
                throw exception;
            }
        }
        */
//            return new LowLevelCStage();
//        } else {
            // return new CudaLowLevelCStage();
        return new SpmdLowLevelCStage();
//        }
    }


    // [end]

    protected Program preprocessProgram(Program lprog, boolean partialEval) {
        return (new PreprocessStage(varGen, options, /* getPreProcStage1(), getIRStage1(), */

                visibleRControl(lprog), partialEval)).visitProgram(lprog);
    }

    
    public SynthesisResult partialEvalAndSolve(Program prog) {
        SketchLoweringResult sketchProg = lowerToSketch(prog);
        // sketchProg.result.debugDump("");
        if (prog.hasFunctions()) {

            SATBackend solver;
            if (options.solverOpts.parallel) {
                solver = new StrategicalBackend(options, internalRControl(), varGen);
            } else {
                solver = new SATBackend(options, internalRControl(), varGen);
            }

            if (options.debugOpts.trace) {
                solver.activateTracing();
            }
            backendParameters();
            solver.partialEvalAndSolve(sketchProg.result);


            return new SynthesisResult(sketchProg, solver.getOracle(),
                    solver.getLastSolutionStats());
        } else {
            return new SynthesisResult(sketchProg, null, null);
        }
    }

    public static class SketchLoweringResult {
        public Program result;
        public Program highLevelC;
        public Program afterSPMDSeq;

        public SketchLoweringResult(Program result, Program highLevelC,
                Program afterSPMDSeq)
        {
            this.result = result;
            this.highLevelC = highLevelC;
            this.afterSPMDSeq = afterSPMDSeq;
        }
    }

    public static class SynthesisResult {
        public SketchLoweringResult lowered;
        public ValueOracle solution;
        public SolutionStatistics solverStats;

        public SynthesisResult(SketchLoweringResult lowered, ValueOracle solution,
                SolutionStatistics solverStats)
        {
            this.lowered = lowered;
            this.solution = solution;
            this.solverStats = solverStats;
        }
    }

    /**
     * Lower the source code to SKETCH, returning a new program and two intermediate ones.
     */
    protected SketchLoweringResult lowerToSketch(Program prog) {
        Program highLevelC = prog;
        prog = getIRStage2_LLC(prog).run(prog);
        Program afterSPMDSeq = prog;
        // prog = getIRStage3().run(prog);
        StencilTransforms stenTf = new StencilTransforms(varGen, options);
        prog = (new LowerToSketch(varGen, options, stenTf)).visitProgram(prog);
        return new SketchLoweringResult(prog, highLevelC, afterSPMDSeq);
    }

	public void testProg(Program p){
	    
        p =
                (Program) p.accept(new EliminateStructs(varGen, new ExprConstInt(
                        options.bndOpts.arrSize)));
        p = (Program) p.accept(new EliminateMultiDimArrays(true, varGen));
	    sketch.compiler.dataflow.nodesToSB.ProduceBooleanFunctions partialEval =
            new sketch.compiler.dataflow.nodesToSB.ProduceBooleanFunctions(varGen,
                    null, System.out
                    , options.bndOpts.unrollAmnt 
                    , options.bndOpts.arrSize
, new AdvancedRControl(
                                options.bndOpts.branchAmnt, options.bndOpts.inlineAmnt,
                                true, p), false);
        log("MAX LOOP UNROLLING = " + options.bndOpts.unrollAmnt);
        log("MAX FUNC INLINING  = " + options.bndOpts.inlineAmnt);
        log("MAX ARRAY SIZE  = " + options.bndOpts.arrSize);
        p.accept(partialEval);
	    
	}

    public static String getOutputFileName(SketchOptions options) {
        if (options.feOpts.outputProgName == null) {
            options.feOpts.outputProgName = options.sketchName;
        }
        String resultFile = options.feOpts.outputProgName;
		if(resultFile.lastIndexOf("/")>=0)
			resultFile=resultFile.substring(resultFile.lastIndexOf("/")+1);
		if(resultFile.lastIndexOf("\\")>=0)
			resultFile=resultFile.substring(resultFile.lastIndexOf("\\")+1);
		if(resultFile.lastIndexOf(".")>=0)
			resultFile=resultFile.substring(0,resultFile.lastIndexOf("."));
		if(resultFile.lastIndexOf(".sk")>=0)
			resultFile=resultFile.substring(0,resultFile.lastIndexOf(".sk"));
		return resultFile;
	}


    protected void outputCCode(Program prog) {
        if (prog == null) {
            printError("Final code generation encountered error, skipping output");
            return;
        }

        (new OutputCCode(varGen, options)).visitProgram(prog);
	}

    public void outputHoleFunc(String outputHoleFunc, Program prog) {
        if (prog == null) {
            printError("Final code generation encountered error, skipping output");
            return;
        }

        (new OutputHoleFunc(outputHoleFunc)).visitProgram(prog);
    }
	
	public String benchmarkName(){
		String rv = "";
		boolean f = true;
		for(String s : options.args){
			if(!f){rv += "_";}
			rv += s;			
			f = false;
		}
		for (String define : options.feOpts.def) {
		    rv += "_" + define;
		}
		return rv;
	}
	

	protected boolean isSketch (Program p) {
		class hasHoles extends FEReplacer {
			public Object visitExprStar (ExprStar es) {
				throw new ControlFlowException ("yes");
			}
		}
		try {  p.accept (new hasHoles ());  return false;  }
		catch (ControlFlowException cfe) {  return true;  }
	}

    public void generateCode(Program prog) {
        // rename main function so it's not the C main
        Map<String, String> rm = new HashMap<String, String>();
        rm.put("main", "_main");
        prog = (Program) prog.accept(new MethodRename(rm));
        prog = (Program) prog.accept(new EliminateAliasesInRefParams(varGen));
        if (options.feOpts.outputHoleFunc != null) {
            outputHoleFunc(options.feOpts.outputHoleFunc, prog);
        }
        if (options.feOpts.customCodegen == null) {
            outputCCode(prog);
        } else {
            customCodegen(options.feOpts.customCodegen, prog);
        }
    }

    public void customCodegen(String jarfile, Program prog) {

        try {
            JarFile jf = new JarFile(jarfile);
            Enumeration<JarEntry> entries = jf.entries();
            URL[] urls = { new URL("jar:file:" + jarfile + "!/") };
            ClassLoader cl = URLClassLoader.newInstance(urls);
            while (entries.hasMoreElements()) {
                JarEntry je = entries.nextElement();
                if (je.isDirectory() || !je.getName().endsWith(".class")) {
                    continue;
                }
                String className = je.getName().substring(0, je.getName().length() - 6);
                className = className.replace('/', '.');
                Class c = cl.loadClass(className);
                Annotation an = c.getAnnotation(sketch.util.annot.CodeGenerator.class);
                if (an != null) {
                    System.out.println("Class " + className + " is a code generator.");
                    System.out.println("Generating code with " + className);
                    try {
                        FEVisitor fev = (FEVisitor) c.newInstance();
                        prog.accept(fev);
                        return;
                    } catch (IllegalAccessException iae) {
                        System.err.println(iae);
                    } catch (InstantiationException iae) {
                        System.err.println(iae);
                    }
                } else {
                    System.out.println("Class " + className + " is not a code generator");
                }
            }
        } catch (IOException ioe) {
            System.err.println("Jar file " + jarfile + " not found");
            System.err.println(ioe);
        } catch (ClassNotFoundException cnfe) {
            System.err.println(cnfe);
        }
        System.err.println("No code generators were found in file " + jarfile);
    }


    public Program preprocAndSemanticCheck(Program prog) {

        // prog = (Program) prog.accept(new CreateHarnesses(varGen));

        prog = (Program) prog.accept(new ExpandRepeatCases());
        // prog.debugDump();
        prog = (Program) prog.accept(new EliminateMacros());
        // prog.debugDump("af");
        prog = (Program) prog.accept(new ConstantReplacer(null));
        
        prog = (Program) prog.accept(new MinimizeFcnCall());
        
        prog = (Program) prog.accept(new SpmdbarrierCall());
        
        prog = (Program) prog.accept(new PidReplacer());

        prog = (Program) prog.accept(new RemoveFunctionParameters(varGen));

        // prog.debugDump("After RemoveFunctionParameters");

        DisambiguateCallsAndTypeCheck dtc = new DisambiguateCallsAndTypeCheck();
        prog = (Program) prog.accept(dtc);
        // prog.debugDump("After");
        if (!dtc.good) {
            throw new ProgramParseException("Semantic check failed");
        }

        prog = (Program) prog.accept(new EliminateTripleEquals(varGen));

        prog = (Program) prog.accept(new MinimizeFcnCall());

        // prog = (getBeforeSemanticCheckStage()).run(prog);

        if (!options.feOpts.lowOverhead) {
            ParallelCheckOption parallelCheck =
                    isParallel() ? ParallelCheckOption.PARALLEL
                            : ParallelCheckOption.SERIAL;
            (new SemanticCheckPass(parallelCheck, true)).visitProgram(prog);
        }

        prog = preprocessProgram(prog, true); // perform prereq

        return nonnull(prog);
	}

	@CompilerPassDeps(runsBefore = {}, runsAfter = {})
    public class SemanticCheckPass extends FEReplacer {
        private final ParallelCheckOption checkopt;
        protected final boolean isParseCheck;

        public SemanticCheckPass(ParallelCheckOption checkopt, boolean isParseCheck) {
            this.checkopt = checkopt;
            this.isParseCheck = isParseCheck;
        }

        @Override
        public Object visitProgram(Program prog) {
            if (!SemanticChecker.check(prog, checkopt, isParseCheck))
                if (isParseCheck) {
                    throw new ProgramParseException("Semantic check failed");
                } else {
                    throw new InternalSketchException("AST fails second semantic check.");
                }
            return prog;
        }
    }

    protected Program parseProgram() {
        return (new ParseProgramStage(varGen, options)).visitProgram(null);
    }

    public void run() {
        this.log(1, "Benchmark = " + this.benchmarkName());
        Program prog = null;
        try {
            prog = parseProgram();
        } catch (SketchException se) {
            throw se;
        } catch (IllegalArgumentException ia) {
            throw ia;
        } catch (RuntimeException re) {
            throw new ProgramParseException("Sketch failed to parse: " + re.getMessage());
        }
        // Program withoutConstsReplaced = this.preprocAndSemanticCheck(prog, false);
        prog = this.preprocAndSemanticCheck(prog);

        // withoutConstsReplaced =
        // prog =
        // (new LowerToHLC(varGen, options)).visitProgram(withoutConstsReplaced);

        SynthesisResult synthResult = this.partialEvalAndSolve(prog);
        prog = synthResult.lowered.result;

        // prog.debugDump("");
        Program finalCleaned = synthResult.lowered.highLevelC;
        // (Program) (new
        // DeleteInstrumentCalls()).visitProgram(synthResult.lowered.highLevelC);


        // beforeUnvectorizing =
        // (Program) (new DeleteCudaSyncthreads()).visitProgram(beforeUnvectorizing);
        Program substituted;
        if (synthResult.solution != null) {
            substituted =
                (new SubstituteSolution(varGen, options, synthResult.solution)).visitProgram(finalCleaned);
        } else {
            substituted = finalCleaned;
        }

        // substituted.debugDump("after substitution");

        Program substitutedCleaned =
                (new CleanupFinalCode(varGen, options, visibleRControl(finalCleaned))).visitProgram(substituted);


        generateCode(substitutedCleaned);
        this.log(1, "[SKETCH] DONE");
    }

    String solverErrorStr;

    public static boolean isTest = false;

    public static void main(String[] args) {
        System.out.println("SKETCH version " +
                PlatformLocalization.getLocalization().version);
        long beg = System.currentTimeMillis();
        ErrorHandling.checkJavaVersion(1, 6);
        // TODO -- change class names so this is clear
        final SequentialSketchMain sketchmain = new SequentialSketchMain(args);
        PlatformLocalization.getLocalization().setTempDirs();
        int exitCode = 0;
        try {
            sketchmain.run();
        } catch (SketchException e) {
            e.print();
            if (isTest) {
                throw e;
            } else {
                // e.printStackTrace();
                exitCode = 1;
            }
        } catch (java.lang.Error e) {
            ErrorHandling.handleErr(e);
            // necessary for unit tests, etc.
            if (isTest) {
                throw e;
            } else {
                exitCode = 1;
            }
        } catch (RuntimeException e) {
            ErrorHandling.handleErr(e);
            if (isTest) {
                throw e;
            } else {
                if (sketchmain.options.debugOpts.verbosity > 3) {
                    e.printStackTrace();
                }
                exitCode = 1;
            }
        } finally {
            System.out.println("Total time = " + (System.currentTimeMillis() - beg));
        }
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }
}
