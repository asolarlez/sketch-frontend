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


import static sketch.util.DebugOut.printNote;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import sketch.compiler.Directive;
import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.FEVisitor;
import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.StreamSpec;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.ExprStar;
import sketch.compiler.ast.core.typs.TypeStruct;
import sketch.compiler.cmdline.SemanticsOptions.ArrayOobPolicy;
import sketch.compiler.cmdline.SolverOptions.ReorderEncoding;
import sketch.compiler.codegenerators.NodesToC;
import sketch.compiler.codegenerators.NodesToCTest;
import sketch.compiler.codegenerators.NodesToH;
import sketch.compiler.dataflow.cflowChecks.PerformFlowChecks;
import sketch.compiler.dataflow.deadCodeElimination.EliminateDeadCode;
import sketch.compiler.dataflow.eliminateTransAssign.EliminateTransAssns;
import sketch.compiler.dataflow.preprocessor.FlattenStmtBlocks;
import sketch.compiler.dataflow.preprocessor.PreprocessSketch;
import sketch.compiler.dataflow.preprocessor.SimplifyVarNames;
import sketch.compiler.dataflow.preprocessor.TypeInferenceForStars;
import sketch.compiler.dataflow.recursionCtrl.AdvancedRControl;
import sketch.compiler.dataflow.recursionCtrl.DelayedInlineRControl;
import sketch.compiler.dataflow.recursionCtrl.RecursionControl;
import sketch.compiler.dataflow.simplifier.ScalarizeVectorAssignments;
import sketch.compiler.main.PlatformLocalization;
import sketch.compiler.parser.StreamItParser;
import sketch.compiler.passes.cleanup.CleanupRemoveMinFcns;
import sketch.compiler.passes.cleanup.MakeCastsExplicit;
import sketch.compiler.passes.cleanup.RemoveTprint;
import sketch.compiler.passes.lowering.*;
import sketch.compiler.passes.lowering.ProtectArrayAccesses.FailurePolicy;
import sketch.compiler.passes.optimization.ReplaceMinLoops;
import sketch.compiler.passes.preprocessing.ForbidArrayAssignmentInFcns;
import sketch.compiler.passes.preprocessing.MainMethodCreateNospec;
import sketch.compiler.passes.preprocessing.MethodRename;
import sketch.compiler.passes.printers.SimpleCodePrinter;
import sketch.compiler.solvers.SATBackend;
import sketch.compiler.solvers.SolutionStatistics;
import sketch.compiler.solvers.constructs.AbstractValueOracle;
import sketch.compiler.solvers.constructs.StaticHoleTracker;
import sketch.compiler.solvers.constructs.ValueOracle;
import sketch.compiler.stencilSK.EliminateStarStatic;
import sketch.compiler.stencilSK.FunctionalizeStencils;
import sketch.compiler.stencilSK.MatchParamNames;
import sketch.compiler.stencilSK.preprocessor.ReplaceFloatsWithBits;
import sketch.util.ControlFlowException;
import sketch.util.Pair;
import sketch.util.exceptions.ProgramParseException;
import sketch.util.exceptions.SketchException;



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
    protected Program beforeUnvectorizing = null;

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

/**
 * This function produces a recursion control that is used by all the user visible transformations.
 * @return
 */
	public RecursionControl visibleRControl() {
		return visibleRControl (prog);
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

	protected Program stencilTransforms(Program p){
	    
	    p = (Program) p.accept(new MatchParamNames());
	    
	    p = (Program)p.accept(new EliminateNestedArrAcc(true));
	    
        
	    //dump(p, "BEFORE Stencilification");
	    FunctionalizeStencils fs = new FunctionalizeStencils(varGen);

        p = (Program)p.accept(fs); //convert Function's to ArrFunction's

        p = fs.processFuns(p, varGen); //process the ArrFunction's and create new Function's
        //dump(p);
        return p;
	}
	
	
	/**
	 * Transform front-end code to have the Java syntax.  Goes through
	 * a series of lowering passes to convert an IR tree from the
	 * "new" syntax to the "old" Java syntax understood by the main
	 * StreamIt compiler.  Conversion directed towards the StreamIt
	 * Java library, as opposed to the compiler, has slightly
	 * different output, mostly centered around phased filters.
	 *
	 * @param libraryFormat  true if the program is being converted
	 *        to run under the StreamIt Java library
	 * @param varGen  object to generate unique temporary variable names
	 * @returns the converted IR tree
	 */
	public void lowerIRToJava()
	{
		prog = (Program)prog.accept(new EliminateBitSelector(varGen));
		
		prog = (Program)prog.accept(new EliminateArrayRange(varGen));
		beforeUnvectorizing = prog;
		
		prog = (new IRStage2()).run(prog);
								
		// prog = (Program)prog.accept (new BoundUnboundedLoops (varGen, params.flagValue ("unrollamnt")));
		
		prog = (Program)prog.accept(new ReplaceSketchesWithSpecs());
		//dump (prog, "after replskwspecs:");
		
		prog = (Program)prog.accept(new MakeBodiesBlocks());
		// dump (prog, "MBB:");
		prog = (Program)prog.accept(new EliminateStructs(varGen, options.bndOpts.heapSize));
		
		prog = (Program)prog.accept(new DisambiguateUnaries(varGen));

		
		
		prog = stencilTransforms(prog);
		
		prog = (Program)prog.accept(new EliminateMultiDimArrays(varGen)); 
		
		prog = (Program)prog.accept(new ExtractRightShifts(varGen));
		//dump (prog, "Extract Vectors in Casts:");
		prog = (Program)prog.accept(new ExtractVectorsInCasts(varGen));
		//dump (prog, "Extract Vectors in Casts:");
		prog = (Program)prog.accept(new SeparateInitializers());
		//dump (prog, "SeparateInitializers:");
		//prog = (Program)prog.accept(new NoRefTypes());
		prog = (Program)prog.accept(new ScalarizeVectorAssignments(varGen, true));
		// dump (prog, "ScalarizeVectorAssns");
		
		
		
		
        
        prog = (Program) prog.accept(new ReplaceFloatsWithBits(varGen));
		
		// By default, we don't protect array accesses in SKETCH
		if (options.semOpts.arrayOobPolicy == ArrayOobPolicy.assertions)
			prog = (Program) prog.accept(new ProtectArrayAccesses(
					FailurePolicy.ASSERTION, varGen));

		// dump (prog, "After protecting array accesses.");
		
		prog = (Program)prog.accept(new EliminateNestedArrAcc(options.semOpts.arrayOobPolicy == ArrayOobPolicy.assertions));
		
		
		
		if (showPhaseOpt("lowering")) {
            dump(prog, "Lowering the code previous to Symbolic execution.");
        }

	}


	protected TempVarGen varGen = new TempVarGen();
	protected Program prog = null;
	protected AbstractValueOracle oracle;
	protected Program finalCode;

	public Program parseProgram(){
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

    public class PreProcStage1 extends CompilerStage {
        public PreProcStage1() {
            super(SequentialSketchMain.this);
            FEVisitor[] passes2 =
                    { new ForbidArrayAssignmentInFcns(), new ReplaceMinLoops(varGen),
                            new MainMethodCreateNospec() };
            passes = passes2;
        }
    }

    public class IRStage1 extends CompilerStage {
        public IRStage1() {
            super(SequentialSketchMain.this);
            FEVisitor[] passes2 = { new GlobalsToParams(varGen) };
            passes = passes2;
        }
    }

    public class IRStage2 extends CompilerStage {
        public IRStage2() {
            super(SequentialSketchMain.this);
            FEVisitor[] passes2 = { new RemoveTprint() };
            passes = passes2;
        }
    }

    public class CleanupStage extends CompilerStage {
        public CleanupStage() {
            super(SequentialSketchMain.this);
            FEVisitor[] passes2 = { new CleanupRemoveMinFcns() };
            passes = passes2;
        }
    }

    protected Program preprocessProgram(Program lprog) {
        boolean useInsertEncoding =
                (options.solverOpts.reorderEncoding == ReorderEncoding.exponential);
		//invoke post-parse passes

		//dump (lprog, "before:");
		lprog = (Program)lprog.accept(new SeparateInitializers ());
		
		lprog = (Program)lprog.accept(new BlockifyRewriteableStmts ());

		lprog = (Program)lprog.accept(new ExtractComplexLoopConditions (varGen));
		lprog = (Program)lprog.accept(new EliminateRegens(varGen));
		lprog = (new PreProcStage1()).run(lprog);
		
		//dump (lprog, "extract clc");
		// lprog = (Program)lprog.accept (new BoundUnboundedLoops (varGen, params.flagValue ("unrollamnt")));
		
		// prog = (Program)prog.accept(new NoRefTypes());
		lprog = (Program)lprog.accept(new EliminateReorderBlocks(varGen, useInsertEncoding));
		//dump (lprog, "~reorderblocks:");
		lprog = (Program)lprog.accept(new EliminateInsertBlocks(varGen));
		//dump (lprog, "~insertblocks:");		
		
		lprog = (Program)lprog.accept(new DisambiguateUnaries(varGen));
		
		lprog = (Program)lprog.accept(new FunctionParamExtension(true));
		// dump (lprog, "fpe:");
		

        lprog = (new IRStage1()).run(lprog);
        
        lprog = (Program) lprog.accept(new TypeInferenceForStars());
        //dump (lprog, "tifs:");

		lprog.accept(new PerformFlowChecks());
		
		lprog = (Program)lprog.accept(new EliminateNestedArrAcc(options.semOpts.arrayOobPolicy == ArrayOobPolicy.assertions));		 
		
		//dump (lprog, "before emd:");
		// lprog = (Program) lprog.accept (new EliminateMultiDimArrays (varGen));
		lprog = (Program) lprog.accept (new MakeMultiDimExplicit(varGen));
		//dump (lprog, "after emd:");
		
		lprog = (Program) lprog.accept(new PreprocessSketch(varGen,
                        options.bndOpts.unrollAmnt, visibleRControl(lprog)));
		
		//dump (lprog, "fpe:");
		
        if (showPhaseOpt("preproc")) {
            dump(lprog, "After Preprocessing");
        }

		return lprog;
	}


    
	public SolutionStatistics partialEvalAndSolve(){
		lowerIRToJava();
		SATBackend solver = new SATBackend(options, internalRControl(), varGen);
		
        if (options.debugOpts.trace) {
            solver.activateTracing();
        }
		backendParameters();
		solver.partialEvalAndSolve(prog);
		
		oracle =solver.getOracle();
		return solver.getLastSolutionStats();
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
	
	public void eliminateStar(){
	    EliminateStarStatic eliminate_star = new EliminateStarStatic(oracle);
		finalCode=(Program)beforeUnvectorizing.accept(eliminate_star);
        
		if (options.feOpts.outputXml != null) {
            eliminate_star.dump_xml(options.feOpts.outputXml);
        }
        this.debugShowPhase("resolve", "after resolving and substituting ?? values",
                finalCode);

        //testProg(finalCode);
		//dump(finalCode, "after elim star");
        finalCode = (Program) finalCode.accept(new PreprocessSketch(varGen,
                        options.bndOpts.unrollAmnt, visibleRControl(), true));
		//dump(finalCode, "After partially evaluating generated code.");
		finalCode = (Program)finalCode.accept(new FlattenStmtBlocks());
        if (showPhaseOpt("postproc")) {
            dump(finalCode, "After Flattening.");
        }
        finalCode = (Program)finalCode.accept(new MakeCastsExplicit());
		finalCode = (Program)finalCode.accept(new EliminateTransAssns());
		//System.out.println("=========  After ElimTransAssign  =========");
		if(showPhaseOpt("taelim")) 
			dump(finalCode, "After Eliminating transitive assignments.");
		
        finalCode = (Program) finalCode.accept(new EliminateDeadCode(
                        options.feOpts.keepAsserts));
		
		//System.out.println("=========  After ElimDeadCode  =========");
		finalCode = (Program)finalCode.accept(new SimplifyVarNames());
		finalCode = (Program)finalCode.accept(new AssembleInitializers());
		if (showPhaseOpt("final")) {
            dump(finalCode, "After Dead Code elimination.");
        }
		
		finalCode = (new CleanupStage()).run(finalCode);
	}

	protected String getOutputFileName() {
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

	protected void outputCCode() {


		
		if (!options.feOpts.outputCode) {
            finalCode.accept(new SimpleCodePrinter());
			//System.out.println(hcode);
			//System.out.println(ccode);
		}else{
            String resultFile = getOutputFileName();
            String hcode = (String)finalCode.accept(new NodesToH(resultFile));      
            String ccode = (String)finalCode.accept(new NodesToC(varGen,resultFile));

		    try{
				{

					Writer outWriter = new FileWriter(options.feOpts.outputDir + resultFile + ".h");
					outWriter.write(hcode);
					outWriter.flush();
					outWriter.close();
					outWriter = new FileWriter(options.feOpts.outputDir + resultFile + ".cpp");
					outWriter.write(ccode);
					outWriter.flush();
					outWriter.close();
				}
                if (options.feOpts.outputTest) {
					String testcode=(String)finalCode.accept(new NodesToCTest(resultFile));
					final String outputFname = options.feOpts.outputDir + resultFile + "_test.cpp";
                    Writer outWriter = new FileWriter(outputFname);
					outWriter.write(testcode);
					outWriter.flush();
					outWriter.close();
					Writer outWriter2 = new FileWriter(options.feOpts.outputDir + "script");
					outWriter2.write("#!/bin/sh\n");
					outWriter2.write("if [ -z \"$SKETCH_HOME\" ];\n" +
							"then\n" +
							"echo \"You need to set the \\$SKETCH_HOME environment variable to be the path to the SKETCH distribution; This is needed to find the SKETCH header files needed to compile your program.\" >&2;\n" +
							"exit 1;\n" +
							"fi\n");
					outWriter2.write("g++ -I \"$SKETCH_HOME/include\" -o "+resultFile+" "+resultFile+".cpp "+resultFile+"_test.cpp\n");

					outWriter2.write("./"+resultFile+"\n");
					outWriter2.flush();
					outWriter2.close();
					printNote("Wrote test harness to", outputFname);
				}
			}
			catch (java.io.IOException e){
				throw new RuntimeException(e);
			}
		}
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

	protected Program doBackendPasses(Program prog) {
//		if( false && params.hasFlag("outputcode") ) {
//			prog=(Program) prog.accept(new AssembleInitializers());
//			prog=(Program) prog.accept(new BitVectorPreprocessor(varGen));
//			//prog.accept(new SimpleCodePrinter());
//			prog=(Program) prog.accept(new BitTypeRemover(varGen));
//			prog=(Program) prog.accept(new SimplifyExpressions());
//		}
	    Map<String, String> rm = new HashMap<String, String>();
	    rm.put("main", "_main");	    
		return (Program) prog.accept(new MethodRename(rm));
	}

	public void generateCode(){
		finalCode=doBackendPasses(finalCode);
		outputCCode();
	}

	public void run()
	{
		log(1, "Benchmark = " + benchmarkName());
		parseProgram();
		preprocAndSemanticCheck();
		
		oracle = new ValueOracle( new StaticHoleTracker(varGen)/* new SequentialHoleTracker(varGen) */);
		partialEvalAndSolve();
		eliminateStar();

		generateCode();
		log(1, "[SKETCH] DONE");

	}

	public void preprocAndSemanticCheck() {
	    prog = (Program)prog.accept(new ConstantReplacer(null));
        if (!SemanticChecker.check(prog, isParallel()))
            throw new ProgramParseException("Semantic check failed");

		prog=preprocessProgram(prog); // perform prereq transformations
		//prog.accept(new SimpleCodePrinter());
		// RenameBitVars is buggy!! prog = (Program)prog.accept(new RenameBitVars());
		// if (!SemanticChecker.check(prog))
		//	throw new IllegalStateException("Semantic check failed");

		if (prog == null)
			throw new IllegalStateException();
	}

    String solverErrorStr;

	public static void checkJavaVersion(int... gt_tuple) {
        String java_version = System.getProperty("java.version");
        String[] version_numbers = java_version.split("\\.");
        for (int a = 0; a < gt_tuple.length; a++) {
            int real_version = Integer.parseInt(version_numbers[a]);
            if (real_version < gt_tuple[a]) {
                String required = "";
                for (int c = 0; c < gt_tuple.length; c++) {
                    required +=
                            String.valueOf(gt_tuple[c])
                                    + ((c != gt_tuple.length - 1) ? "." : "");
                }
                System.err.println("your java version is out of date. Version "
                        + required + " required");
                System.exit(1);
            }
        }
    }

    public static void main(String[] args) {
        long beg = System.currentTimeMillis();
        checkJavaVersion(1, 6);
        final SequentialSketchMain sketchmain = new SequentialSketchMain(args);
        try {
            sketchmain.run();
        } catch (SketchException e) {
            e.print();
            System.exit(1);
        } catch (RuntimeException e) {
            System.err.println("[ERROR] [SKETCH] Failed with " +
                    e.getClass().getSimpleName() + " exception; message: " +
                    e.getMessage());
            if (sketchmain.prog == null) {
                System.err.println("[ERROR] [SKETCH]     program null.");
            } else {
                try {
                    final PlatformLocalization loc =
                            PlatformLocalization.getLocalization();
                    File out_file = loc.getTempPath("error-last-program.txt");
                    sketchmain.prog.debugDump(out_file);
                    System.err.println("[ERROR] [SKETCH]     program dumped to: " + out_file);
                } catch (Throwable e2) {}
            }
            // necessary for unit tests, etc.
            throw e;
        }
        System.out.println("Total time = " + (System.currentTimeMillis() - beg));
    }
}
