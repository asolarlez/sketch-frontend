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


import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import sketch.compiler.Directive;
import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.FEVisitor;
import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.StreamSpec;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.ExprStar;
import sketch.compiler.ast.core.typs.TypeStruct;
import sketch.compiler.dataflow.recursionCtrl.AdvancedRControl;
import sketch.compiler.dataflow.recursionCtrl.DelayedInlineRControl;
import sketch.compiler.dataflow.recursionCtrl.RecursionControl;
import sketch.compiler.main.cuda.CudaSketchMain;
import sketch.compiler.main.other.ErrorHandling;
import sketch.compiler.main.passes.LowerToHLC;
import sketch.compiler.main.passes.LowerToSketch;
import sketch.compiler.main.passes.OutputCCode;
import sketch.compiler.main.passes.PreprocessStage;
import sketch.compiler.main.passes.RunPrintFunctions;
import sketch.compiler.main.passes.StencilTransforms;
import sketch.compiler.main.passes.SubstituteSolution;
import sketch.compiler.parser.StreamItParser;
import sketch.compiler.passes.annotations.CompilerPassDeps;
import sketch.compiler.passes.cleanup.CleanupRemoveMinFcns;
import sketch.compiler.passes.cleanup.RemoveTprint;
import sketch.compiler.passes.cuda.ReplaceParforLoops;
import sketch.compiler.passes.lowering.ConstantReplacer;
import sketch.compiler.passes.lowering.EliminateMultiDimArrays;
import sketch.compiler.passes.lowering.EliminateStructs;
import sketch.compiler.passes.lowering.GlobalsToParams;
import sketch.compiler.passes.lowering.ReplaceImplicitVarDecl;
import sketch.compiler.passes.lowering.SemanticChecker;
import sketch.compiler.passes.lowering.SemanticChecker.ParallelCheckOption;
import sketch.compiler.passes.optimization.ReplaceMinLoops;
import sketch.compiler.passes.preprocessing.AllthreadsTprintFcnCall;
import sketch.compiler.passes.preprocessing.MainMethodCreateNospec;
import sketch.compiler.passes.preprocessing.MethodRename;
import sketch.compiler.passes.preprocessing.MinimizeFcnCall;
import sketch.compiler.passes.preprocessing.SetDeterministicFcns;
import sketch.compiler.passes.preprocessing.TprintFcnCall;
import sketch.compiler.passes.preprocessing.WarnAmbiguousImplicitVarDecl;
import sketch.compiler.solvers.SATBackend;
import sketch.compiler.solvers.SolutionStatistics;
import sketch.compiler.solvers.constructs.ValueOracle;
import sketch.compiler.stencilSK.EliminateStarStatic;
import sketch.util.ControlFlowException;
import sketch.util.Pair;
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
        super(new SequentialSketchOptions(args));
    }

	/** for subclasses */
    public SequentialSketchMain(SequentialSketchOptions options) {
        super(options);
    }

    public boolean isParallel () {
		return false;
	}

    public RecursionControl visibleRControl (Program p) {
		// return new BaseRControl(params.inlineAmt);
		return new AdvancedRControl(options.bndOpts.branchAmnt, options.bndOpts.inlineAmnt, p);
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


	/**
	 * Generate a Program object that includes built-in structures
	 * and streams with code, but no user code.
	 *
	 * @returns a StreamIt program containing only built-in code
	 */
	public static Program emptyProgram()
	{
		List<StreamSpec> streams = new java.util.ArrayList<StreamSpec>();
		List<TypeStruct> structs = new java.util.ArrayList<TypeStruct>();

		// Complex structure type:
//		List<String> fields = new java.util.ArrayList<String>();
//		List<Type> ftypes = new java.util.ArrayList<Type>();

		// We don't support the Complex type in SKETCH
//			Type floattype = TypePrimitive.floattype ;
//			fields.add("real");
//			ftypes.add(floattype);
//			fields.add("imag");
//			ftypes.add(floattype);
//			TypeStruct complexStruct =
//				new TypeStruct(null, "Complex", fields, ftypes);
//			structs.add(complexStruct);

		return new Program(null, streams, structs);
	}

	/**
	 * Read, parse, and combine all of the StreamIt code in a list of
	 * files.  Reads each of the files in <code>inputFiles</code> in
	 * turn and runs <code>sketch.compiler.StreamItParserFE</code>
	 * over it.  This produces a
	 * <code>sketch.compiler.nodes.Program</code> containing lists
	 * of structures and streams; combine these into a single
	 * <code>sketch.compiler.nodes.Program</code> with all of the
	 * structures and streams.
	 *
	 * @param inputFiles  list of strings naming the files to be read
	 * @returns a representation of the entire program, composed of the
	 *          code in all of the input files
	 * @throws java.io.IOException if an error occurs reading the input
	 *         files
	 * @throws antlr.RecognitionException if an error occurs parsing
	 *         the input files; that is, if the code is syntactically
	 *         incorrect
	 * @throws antlr.TokenStreamException if an error occurs producing
	 *         the input token stream
	 */
	public Pair<Program, Set<Directive>> parseFiles(List<String> inputFiles)
	throws java.io.IOException, antlr.RecognitionException, antlr.TokenStreamException
	{
		Program prog = emptyProgram();
		boolean useCpp = true;
		List<String> cppDefs = Arrays.asList(options.feOpts.def);
		Set<Directive> pragmas = new HashSet<Directive> ();

		for (String inputFile : inputFiles) {
			StreamItParser parser = new StreamItParser (inputFile, useCpp, cppDefs);
			Program pprog = parser.parse ();
			if (pprog==null)
				return null;

			List<StreamSpec> newStreams = new java.util.ArrayList<StreamSpec> ();
			List<TypeStruct> newStructs = new java.util.ArrayList<TypeStruct> ();
			newStreams.addAll(prog.getStreams());
			newStreams.addAll(pprog.getStreams());
			newStructs.addAll(prog.getStructs());
			newStructs.addAll(pprog.getStructs());
			pragmas.addAll (parser.getDirectives ());
			prog = new Program(null, newStreams, newStructs);
		}
		return new Pair<Program, Set<Directive>> (prog, pragmas);
	}

	protected TempVarGen varGen = new TempVarGen();

	public Program parseProgram(){
        Program prog;
		try
		{
            Pair<Program, Set<Directive>> res = parseFiles(options.argsAsList);
            if (res == null) {
                throw new ProgramParseException("could not parse program");
            }
            prog = res.getFirst();
            processDirectives(res.getSecond());

            if (showPhaseOpt("parse"))
                dump(prog, "After parsing");
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
			//e.printStackTrace(System.err);
			throw new RuntimeException(e);
		}

        if (prog == null) {
            throw new ProgramParseException("Compilation didn't generate a parse tree.");
        }
        return prog;

	}

	/** hack to check deps across stages; accessed by CompilerStage */
    public final HashSet<Class<? extends FEVisitor>> runClasses =
            new HashSet<Class<? extends FEVisitor>>();
    
    public class BeforeSemanticCheckStage extends CompilerStage {
        public BeforeSemanticCheckStage() {
            super(SequentialSketchMain.this);
            FEVisitor[] passes2 =
                    { new MinimizeFcnCall(), new TprintFcnCall(),
                            new AllthreadsTprintFcnCall() };
            passes = new Vector<FEVisitor>(Arrays.asList(passes2));
        }
    }

    public class PreProcStage1 extends CompilerStage {
        public PreProcStage1() {
            super(SequentialSketchMain.this);
            FEVisitor[] passes2 =
                    { new WarnAmbiguousImplicitVarDecl(), new ReplaceMinLoops(varGen),
                            new MainMethodCreateNospec(),
                            new SetDeterministicFcns(),
                            new ReplaceParforLoops(options.getCudaBlockDim(), varGen),
                            new ReplaceImplicitVarDecl() };
            passes = new Vector<FEVisitor>(Arrays.asList(passes2));
        }
    }

    public class IRStage1 extends CompilerStage {
        public IRStage1() {
            super(SequentialSketchMain.this);
            FEVisitor[] passes2 = { new GlobalsToParams(varGen)
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
    public class IRStage2_LLC extends CompilerStage {
        public IRStage2_LLC() {
            super(SequentialSketchMain.this);
            FEVisitor[] passes2 = { };
            passes = new Vector<FEVisitor>(Arrays.asList(passes2));
        }
    }

    public class IRStage3 extends CompilerStage {
        public IRStage3() {
            super(SequentialSketchMain.this);
            FEVisitor[] passes2 = { new RemoveTprint() };
            passes = new Vector<FEVisitor>(Arrays.asList(passes2));
        }
    }

    public class CleanupStage extends CompilerStage {
        public CleanupStage() {
            super(SequentialSketchMain.this);
            FEVisitor[] passes2 = { new CleanupRemoveMinFcns() };
            passes = new Vector<FEVisitor>(Arrays.asList(passes2));
        }
    }

    // [start] function overloads for stage classes
    public BeforeSemanticCheckStage getBeforeSemanticCheckStage() {
        return new BeforeSemanticCheckStage();
    }
    
    public PreProcStage1 getPreProcStage1() {
        return new PreProcStage1();
    }

    public IRStage1 getIRStage1() {
        return new IRStage1();
    }

    public IRStage2_LLC getIRStage2_LLC() {
        return new IRStage2_LLC();
    }

    public IRStage3 getIRStage3() {
        return new IRStage3();
    }

    public CleanupStage getCleanupStage() {
        return new CleanupStage();
    }
    // [end]

    protected Program preprocessProgram(Program lprog, boolean partialEval) {
        return (new PreprocessStage(varGen, options, getPreProcStage1(), getIRStage1(),
                visibleRControl(lprog), partialEval)).visitProgram(lprog);
    }

    
    public SynthesisResult partialEvalAndSolve(Program prog) {
        SketchLoweringResult sketchProg = lowerToSketch(prog);

        SATBackend solver = new SATBackend(options, internalRControl(), varGen);

        if (options.debugOpts.trace) {
            solver.activateTracing();
        }
        backendParameters();
        solver.partialEvalAndSolve(sketchProg.result);

        return new SynthesisResult(sketchProg, solver.getOracle(),
                solver.getLastSolutionStats());
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
        prog = (new LowerToHLC(varGen, options)).visitProgram(prog);
        Program highLevelC = prog;
        prog = getIRStage2_LLC().run(prog);
        Program afterSPMDSeq = prog;
        prog = getIRStage3().run(prog);
        StencilTransforms stenTf = new StencilTransforms(varGen, options);
        prog = (new LowerToSketch(varGen, options, stenTf)).visitProgram(prog);
        return new SketchLoweringResult(prog, highLevelC, afterSPMDSeq);
    }

	public void testProg(Program p){
	    
	    p = (Program)p.accept(new EliminateStructs(varGen, options.bndOpts.heapSize));
	    p = (Program)p.accept(new EliminateMultiDimArrays(varGen));
	    sketch.compiler.dataflow.nodesToSB.ProduceBooleanFunctions partialEval =
            new sketch.compiler.dataflow.nodesToSB.ProduceBooleanFunctions(varGen,
                    null, System.out
                    , options.bndOpts.unrollAmnt 
                    , options.bndOpts.arrSize
                    ,new AdvancedRControl(options.bndOpts.branchAmnt, options.bndOpts.inlineAmnt, p ), false);
        log("MAX LOOP UNROLLING = " + options.bndOpts.unrollAmnt);
        log("MAX FUNC INLINING  = " + options.bndOpts.inlineAmnt);
        log("MAX ARRAY SIZE  = " + options.bndOpts.arrSize);
        p.accept(partialEval);
	    
	}

    public static String getOutputFileName(SequentialSketchOptions options) {
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

    protected void runPrintFunctions(SketchLoweringResult llc, ValueOracle oracle) {
        EliminateStarStatic eliminate_star = new EliminateStarStatic(oracle);
        Program serializedCode = (Program) llc.afterSPMDSeq.accept(eliminate_star);
        (new RunPrintFunctions(varGen, options)).visitProgram(serializedCode);
    }

    protected void outputCCode(Program prog) {
        if (prog == null) {
            printError("Final code generation encountered error, skipping output");
            return;
        }

        (new OutputCCode(varGen, options)).visitProgram(prog);
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

        outputCCode(prog);
    }

    // NOTE: This function is not used, see CudaSketchMain!!
	public void run()
	{
		log(1, "Benchmark = " + benchmarkName());
        Program prog = parseProgram();
        prog = preprocAndSemanticCheck(prog, true);
		
        SynthesisResult synthResult = partialEvalAndSolve(prog);
        prog = synthResult.lowered.result;
		runPrintFunctions(synthResult.lowered, synthResult.solution);
		
        Program substituted =
                (new SubstituteSolution(varGen, options, synthResult.solution,
                        visibleRControl(prog))).visitProgram(prog);
        substituted = (getCleanupStage()).run(prog);

        generateCode(substituted);
		log(1, "[SKETCH] DONE");

	}

    public Program preprocAndSemanticCheck(Program prog, boolean replaceConstants) {
        if (replaceConstants) {
            prog = (Program) prog.accept(new ConstantReplacer(null));
        }
	    prog = (getBeforeSemanticCheckStage()).run(prog);
	    ParallelCheckOption parallelCheck = isParallel() ? ParallelCheckOption.PARALLEL : ParallelCheckOption.SERIAL;
        (new SemanticCheckPass(parallelCheck, true)).visitProgram(prog);
        this.showPhaseOpt("parse");

        prog = preprocessProgram(prog, replaceConstants); // perform prereq
                                                          // transformations
		//prog.accept(new SimpleCodePrinter());
		// RenameBitVars is buggy!! prog = (Program)prog.accept(new RenameBitVars());
		// if (!SemanticChecker.check(prog))
		//	throw new IllegalStateException("Semantic check failed");

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

    String solverErrorStr;

    public static boolean isTest = false;

    public static void main(String[] args) {
        System.out.println("SKETCH version features: tprint, cuda-model, vlarrays");
        long beg = System.currentTimeMillis();
        ErrorHandling.checkJavaVersion(1, 6);
        // TODO -- change class names so this is clear
        final SequentialSketchMain sketchmain = new CudaSketchMain(args);
        try {
            sketchmain.run();
        } catch (SketchException e) {
            e.print();
            ErrorHandling.dumpProgramToFile(null);
            if (isTest) {
                throw e;
            } else {
                System.exit(1);
            }
        } catch (java.lang.Error e) {
            ErrorHandling.handleErr(null, e);
            // necessary for unit tests, etc.
            throw e;
        } catch (RuntimeException e) {
            ErrorHandling.handleErr(null, e);
            throw e;
        }
        System.out.println("Total time = " + (System.currentTimeMillis() - beg));
    }
}
