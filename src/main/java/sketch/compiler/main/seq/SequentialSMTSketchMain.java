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

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import sketch.compiler.Directive;
import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.StreamSpec;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.ExprStar;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.ast.core.typs.TypeStruct;
import sketch.compiler.cmdline.SMTOptions.IntModel;
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
import sketch.compiler.dataflow.recursionCtrl.RecursionControl;
import sketch.compiler.parser.StreamItParser;
import sketch.compiler.passes.lowering.*;
import sketch.compiler.passes.lowering.ProtectArrayAccesses.FailurePolicy;
import sketch.compiler.passes.preprocessing.BitTypeRemover;
import sketch.compiler.passes.preprocessing.BitVectorPreprocessor;
import sketch.compiler.passes.preprocessing.SimplifyExpressions;
import sketch.compiler.passes.printers.SimpleCodePrinter;
import sketch.compiler.smt.CEGISLoop;
import sketch.compiler.smt.GeneralStatistics;
import sketch.compiler.smt.ProduceSMTCode;
import sketch.compiler.smt.partialeval.NodeToSmtVtype;
import sketch.compiler.smt.partialeval.ScalarizeAssignmentNotBitArray;
import sketch.compiler.smt.partialeval.SmtValueOracle;
import sketch.compiler.smt.passes.AddWrapper;
import sketch.compiler.smt.passes.ArithmeticSimplification;
import sketch.compiler.smt.passes.EliminateRegens;
import sketch.compiler.smt.passes.EliminateStarStatic;
import sketch.compiler.smt.passes.FunctionParamExtension;
import sketch.compiler.smt.passes.RegularizeTypesByTypeCheck;
import sketch.compiler.smt.passes.ReplaceStructTypeWithInt;
import sketch.compiler.smt.solvers.SMTBackend;
import sketch.compiler.solvers.constructs.AbstractValueOracle;
import sketch.util.ControlFlowException;
import sketch.util.Pair;

/**
 * Convert StreamIt programs to legal Java code. This is the main entry point
 * for the StreamIt syntax converter. Running it as a standalone program reads
 * the list of files provided on the command line and produces equivalent Java
 * code on standard output or the file named in the <tt>--output</tt>
 * command-line parameter.
 * 
 * @author David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: SequentialSMTSketchMain.java,v 1.46 2009/11/03 22:40:32 lshan Exp $
 */
public class SequentialSMTSketchMain extends CommonSketchMain {
    
	// protected final CommandLineParams params;
	protected Program beforeUnvectorizing = null;
	Program finalCode;
	Program prog = null;
	
	String solverErrorStr;
    private CEGISLoop loop;
    private SMTBackend solver;
    private NodeToSmtVtype vtype;
	
	protected String programName;
	
	public final SMTSketchOptions options;
	private static Logger log = Logger.getLogger(SequentialSMTSketchMain.class.getCanonicalName());
	
	protected TempVarGen varGen = new TempVarGen("__sa");
	
	SmtValueOracle bestOracle;
	
	GeneralStatistics stat;
	
	/*
	 * Getters & Setters
	 */
	public boolean isParallel() {
		return false;
	}

	public AbstractValueOracle getOracle() {
		return this.bestOracle;
	}
	
	public GeneralStatistics getSolutionStat() {
		return this.stat;
	}
	
	public String getProgramName() {
		return this.programName;
	}

	/**
	 * This function produces a recursion control that is used by all the user
	 * visible transformations.
	 * 
	 * @return
	 */
	public RecursionControl visibleRControl() {
		return visibleRControl(prog);
	}
	
	public String getName() {
		return getOutputFileName();
	}

	/*
	 * Constructors
	 */
	public SequentialSMTSketchMain(String[] args) {
	    super(new SMTSketchOptions(args));
	    this.options = (SMTSketchOptions) super.options;
		
		Logger rootLogger = Logger.getLogger("");
		int verbosity = options.debugOpts.verbosity;
		Level vLevel = null;
		switch (verbosity) {
		    case 0: vLevel = Level.OFF; break;
    		case 1: vLevel = Level.SEVERE; break;
    		case 2: vLevel = Level.WARNING; break;
    		case 3: vLevel = Level.INFO; break;
    		case 4: vLevel = Level.FINE; break;
    		case 5: vLevel = Level.FINER; break;
    		case 6: vLevel = Level.FINEST; break;
		}
		rootLogger.setLevel(vLevel);
		
	    Handler[] handlers = rootLogger.getHandlers();
	    SimpleFormatter f = new SimpleFormatter() {
	    	@Override
	    	public synchronized String format(LogRecord record) {
	    		StringBuffer sb = new StringBuffer();
	    		sb.append(record.getLevel());
	    		sb.append(": ");
	    		sb.append(record.getMessage());
	    		sb.append('\n');
	    		return sb.toString();
	    	}
	    };
	   
	    for ( int index = 0; index < handlers.length; index++ ) {
	      handlers[index].setLevel( vLevel );
	      handlers[index].setFormatter(f);
	    }
		
		this.programName = getOutputFileName();
		this.stat = new GeneralStatistics();
	}

	public static RecursionControl visibleRControl(Program p) {
        // return new BaseRControl(params.inlineAmt);
        return new AdvancedRControl(SMTSketchOptions.getSingleton().bndOpts.branchAmnt,
                SMTSketchOptions.getSingleton().bndOpts.inlineAmnt, p);
    }

	/**
	 * This function produces a recursion control that is used by all
	 * transformations that are not user visible. In particular, the conversion
	 * to boolean. By default it is the same as the visibleRControl.
	 * 
	 * @return
	 */
	public RecursionControl internalRControl() {

		return visibleRControl();
	}

	/**
	 * Generate a Program object that includes built-in structures and streams
	 * with code, but no user code.
	 * 
	 * @returns a StreamIt program containing only built-in code
	 */
	public static Program emptyProgram() {
		List<StreamSpec> streams = new java.util.ArrayList<StreamSpec>();
		List<TypeStruct> structs = new java.util.ArrayList<TypeStruct>();

		// Complex structure type:
		List<String> fields = new java.util.ArrayList<String>();
		List<Type> ftypes = new java.util.ArrayList<Type>();

		// We don't support the Complex type in SKETCH
		if (false) {
			Type floattype = TypePrimitive.floattype;
			fields.add("real");
			ftypes.add(floattype);
			fields.add("imag");
			ftypes.add(floattype);
			TypeStruct complexStruct = new TypeStruct(null, "Complex", fields,
					ftypes);
			structs.add(complexStruct);
		}

		return new Program(null, streams, structs);
	}

	/**
	 * Read, parse, and combine all of the StreamIt code in a list of files.
	 * Reads each of the files in <code>inputFiles</code> in turn and runs
	 * <code>streamit.frontend.StreamItParserFE</code> over it. This produces a
	 * <code>streamit.frontend.nodes.Program</code> containing lists of
	 * structures and streams; combine these into a single
	 * <code>streamit.frontend.nodes.Program</code> with all of the structures
	 * and streams.
	 * 
	 * @param inputFiles
	 *            list of strings naming the files to be read
	 * @returns a representation of the entire program, composed of the code in
	 *          all of the input files
	 * @throws java.io.IOException
	 *             if an error occurs reading the input files
	 * @throws antlr.RecognitionException
	 *             if an error occurs parsing the input files; that is, if the
	 *             code is syntactically incorrect
	 * @throws antlr.TokenStreamException
	 *             if an error occurs producing the input token stream
	 */
	public Pair<Program, Set<Directive>> parseFiles(List<String> inputFiles)
			throws java.io.IOException, antlr.RecognitionException,
			antlr.TokenStreamException {
		Program prog = emptyProgram();
		boolean useCpp = true;
		List<String> cppDefs = Arrays.asList(options.feOpts.def);
		Set<Directive> pragmas = new HashSet<Directive>();

		for (String inputFile : inputFiles) {
			StreamItParser parser = new StreamItParser(inputFile, useCpp,
					cppDefs);
			Program pprog = parser.parse();
			if (pprog == null)
				return null;

			List<StreamSpec> newStreams = new java.util.ArrayList<StreamSpec>();
			List<TypeStruct> newStructs = new java.util.ArrayList<TypeStruct>();
			newStreams.addAll(prog.getStreams());
			newStreams.addAll(pprog.getStreams());
			newStructs.addAll(prog.getStructs());
			newStructs.addAll(pprog.getStructs());
			pragmas.addAll(parser.getDirectives());
			prog = new Program(null, newStreams, newStructs);
		}
		return new Pair<Program, Set<Directive>>(prog, pragmas);
	}

	/**
	 * Transform front-end code to have the Java syntax. Goes through a series
	 * of lowering passes to convert an IR tree from the "new" syntax to the
	 * "old" Java syntax understood by the main StreamIt compiler. Conversion
	 * directed towards the StreamIt Java library, as opposed to the compiler,
	 * has slightly different output, mostly centered around phased filters.
	 * 
	 * @param libraryFormat
	 *            true if the program is being converted to run under the
	 *            StreamIt Java library
	 * 
	 * @returns the converted IR tree
	 */
	public Program lowering(Program prog) {
//		prog = (Program) prog.accept(new AddConstraintsVisitor());
		prog = (Program) prog.accept(new EliminateBitSelector(varGen));

		prog = (Program) prog.accept(new EliminateArrayRange(varGen));
		beforeUnvectorizing = prog;
//		dump(prog, "After eliminting array range");
		
		prog = (Program) prog.accept(new MakeBodiesBlocks());
		// dump (prog, "MBB:");
        prog = (Program) prog.accept(new EliminateStructs(varGen,
                        options.bndOpts.heapSize));
//		dump (prog, "Before ReplaceStructTypeWithInt:");
		prog = (Program) prog.accept(new ReplaceStructTypeWithInt());
//		dump (prog, "After ReplaceStructTypeWithInt:");
		
		prog = (Program) prog.accept(new DisambiguateUnaries(varGen));
//		dump (prog, "After eliminating structs:");
		prog = (Program) prog.accept(new EliminateMultiDimArrays(varGen));
//		dump (prog, "After second elimination of multi-dim arrays:");
		prog = (Program) prog.accept(new ExtractRightShifts(varGen));
//		dump (prog, "After ExtractRightShifts");
		prog = (Program) prog.accept(new ExtractVectorsInCasts(varGen));
//		dump (prog, "After ExtractVectorsInCasts");
		prog = (Program) prog.accept(new SeparateInitializers());
//		dump (prog, "SeparateInitializers:");
		// prog = (Program)prog.accept(new NoRefTypes());
		
//		prog = (Program) prog.accept(new ScalarizeVectorAssignments(varGen,
//				true));
		
		
		prog = (Program) prog.accept(new ScalarizeAssignmentNotBitArray(varGen,
				true));
		
//		dump (prog, "After ScalarizeVectorAssignments");

		// By default, we don't protect array accesses in SKETCH
		if (options.semOpts.arrayOobPolicy == ArrayOobPolicy.assertions) {
			prog = (Program) prog.accept(new ProtectArrayAccesses(
					FailurePolicy.ASSERTION, varGen));
//			dump (prog, "After arrayOOBPolicy");
		}

//		dump (prog, "Before SeparateInitializers:");
		prog = (Program) prog.accept(new SeparateInitializers());
//		dump (prog, "Before AddWrapper:");
		prog = (Program) prog.accept(new AddWrapper());
//		dump (prog, "Before regularize types:");
		prog = (Program) prog.accept(new RegularizeTypesByTypeCheck());
		
        if (showPhaseOpt("lowering"))
            dump(prog, "Lowering the code previous to Symbolic execution.");

		prog = (Program) prog.accept(new EliminateNestedArrAcc(true));
//		 dump (prog, "After lowerIR:");
		return prog;
	}

	public Program parseProgram() {
		
		try {
			Pair<Program, Set<Directive>> res = parseFiles(options.argsAsList);
			prog = res.getFirst();
			processDirectives(res.getSecond());
			
			if (showPhaseOpt("parse"))
				dump(prog, "After parsing");
		} catch (Exception e) {
			// e.printStackTrace(System.err);
			throw new RuntimeException(e);
		}

		if (prog == null) {
			System.err.println("Compilation didn't generate a parse tree.");
			throw new IllegalStateException();
		}
		return prog;
    }

	protected Program preprocessProgram(Program lprog) {
	    boolean useInsertEncoding =
            (options.solverOpts.reorderEncoding == ReorderEncoding.exponential);
		// invoke post-parse passes

//		dump (prog, "before:");
//		lprog = (Program) lprog.accept(new RenameInputVars());
		
		lprog = (Program) lprog.accept(new SeparateInitializers());
		lprog = (Program) lprog.accept(new BlockifyRewriteableStmts());
		
		lprog = (Program)lprog.accept(new ExtractComplexLoopConditions (varGen));
		lprog = (Program) lprog.accept(new EliminateRegens(varGen));
//		lprog = (Program) lprog.accept(new EliminateTernery(varGen));
//		dump (lprog, "~regens");

		// prog = (Program)prog.accept(new NoRefTypes());
		lprog = (Program) lprog.accept(new EliminateReorderBlocks(varGen,
				useInsertEncoding));
//		 dump (lprog, "~reorderblocks:");
		lprog = (Program) lprog.accept(new EliminateInsertBlocks(varGen));
		// dump (lprog, "~insertblocks:");
//		lprog = (Program) lprog.accept(new BoundUnboundedLoops(varGen, params.flagValue("unrollamnt")));
		
		// dump (lprog, "bounded loops");
//		dump (lprog, "bef fpe:");
		lprog = (Program) lprog.accept(new FunctionParamExtension(true));
//		dump (lprog, "fpe:");
		lprog = (Program) lprog.accept(new DisambiguateUnaries(varGen));
//		dump (lprog, "tifs:");
		lprog = (Program) lprog.accept(new TypeInferenceForStars());
//		 dump (lprog, "tifs:");
		
		lprog.accept(new PerformFlowChecks());
		
		lprog = (Program) lprog.accept(new EliminateMultiDimArrays(varGen));
//		dump (lprog, "After first elimination of multi-dim arrays:");
        lprog = (Program) lprog.accept(new PreprocessSketch(varGen,
                        options.bndOpts.unrollAmnt, visibleRControl(lprog)));
        if (showPhaseOpt("preproc"))
            dump(lprog, "After Preprocessing");
//		dump(lprog, "After Preprocessing");
		return lprog;
	}

	public void eliminateStar(){
		finalCode=(Program)beforeUnvectorizing.accept(new EliminateStarStatic(bestOracle));
//		 dump(finalCode, "after elim star");
        finalCode = (Program) finalCode.accept(new PreprocessSketch(varGen,
                        options.bndOpts.unrollAmnt, visibleRControl(), true));
		// dump(finalCode, "After partially evaluating generated code.");
		finalCode = (Program)finalCode.accept(new FlattenStmtBlocks());
		if(showPhaseOpt("postproc")) 
			dump(finalCode, "After Flattening.");
		finalCode = (Program)finalCode.accept(new EliminateTransAssns());
		//System.out.println("=========  After ElimTransAssign  =========");
		if(showPhaseOpt("taelim")) 
			dump(finalCode, "After Eliminating transitive assignments.");
		finalCode = (Program)finalCode.accept(new EliminateDeadCode(options.feOpts.keepAsserts));
		//dump(finalCode, "After Dead Code elimination.");
		//System.out.println("=========  After ElimDeadCode  =========");
		finalCode = (Program)finalCode.accept(new SimplifyVarNames());
		finalCode = (Program)finalCode.accept(new AssembleInitializers());
		
		finalCode = (Program)finalCode.accept(new ArithmeticSimplification());
		if(showPhaseOpt("final")) 
			dump(finalCode, "After Dead Code elimination.");
	}

	protected String getOutputFileName() {
		String resultFile = options.feOpts.output;
		if (resultFile == null) {
			resultFile = options.args[0];
		}
		if (resultFile.lastIndexOf("/") >= 0)
			resultFile = resultFile.substring(resultFile.lastIndexOf("/") + 1);
		if (resultFile.lastIndexOf("\\") >= 0)
			resultFile = resultFile.substring(resultFile.lastIndexOf("\\") + 1);
		if (resultFile.lastIndexOf(".") >= 0)
			resultFile = resultFile.substring(0, resultFile.lastIndexOf("."));
		if (resultFile.lastIndexOf(".sk") >= 0)
			resultFile = resultFile.substring(0, resultFile.lastIndexOf(".sk"));
		return resultFile;
	}

	protected void outputCCode() {

		String resultFile = getOutputFileName();
		
		if (!options.feOpts.outputCode) {
			 finalCode.accept( new SimpleCodePrinter() );
			// System.out.println(hcode);
//			System.out.println(ccode);
		} else {
			try {
				{
					String hcode = (String) finalCode.accept(new NodesToH(resultFile));
					String ccode = (String) finalCode.accept(new NodesToC(varGen,
							resultFile));
					
					Writer outWriter = new FileWriter(options.feOpts.outputDir
							+ resultFile + ".h");
					outWriter.write(hcode);
					outWriter.flush();
					outWriter.close();
					outWriter = new FileWriter(options.feOpts.outputDir
							+ resultFile + ".cpp");
					outWriter.write(ccode);
					outWriter.flush();
					outWriter.close();
				}
				if (options.feOpts.outputTest) {
					String testcode = (String) beforeUnvectorizing
							.accept(new NodesToCTest(resultFile));
					Writer outWriter = new FileWriter(options.feOpts.outputDir
							+ resultFile + "_test.cpp");
					outWriter.write(testcode);
					outWriter.flush();
					outWriter.close();
				}
				if (options.feOpts.outputTest) {
					Writer outWriter = new FileWriter(options.feOpts.outputDir
							+ "script");
					outWriter.write("#!/bin/sh\n");
					outWriter
							.write("if [ -z \"$SKETCH_HOME\" ];\n"
									+ "then\n"
									+ "echo \"You need to set the \\$SKETCH_HOME environment variable to be the path to the SKETCH distribution; This is needed to find the SKETCH header files needed to compile your program.\" >&2;\n"
									+ "exit 1;\n" + "fi\n");
					outWriter.write("g++ -I \"$SKETCH_HOME/include\" -src "
							+ resultFile + " " + resultFile + ".cpp "
							+ resultFile + "_test.cpp\n");

					outWriter.write("./" + resultFile + "\n");
					outWriter.flush();
					outWriter.close();
				}
			} catch (java.io.IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	protected boolean isSketch(Program p) {
		class hasHoles extends FEReplacer {
			public Object visitExprStar(ExprStar es) {
				throw new ControlFlowException("yes");
			}
		}
		try {
			p.accept(new hasHoles());
			return false;
		} catch (ControlFlowException cfe) {
			return true;
		}
	}

	protected Program doBackendPasses(Program prog) {
		if (options.feOpts.outputCode) {
			prog = (Program) prog.accept(new AssembleInitializers());
			prog = (Program) prog.accept(new BitVectorPreprocessor(varGen));
			// prog.accept(new SimpleCodePrinter());
			prog = (Program) prog.accept(new BitTypeRemover(varGen));
			prog = (Program) prog.accept(new SimplifyExpressions());
		}
		return prog;
	}

	public void generateCode() {
		finalCode = doBackendPasses(finalCode);
		outputCCode();
	}

	public void run() {
		try {
			if (runBeforeGenerateCode()) {
				
				log.info(stat.toString());
				if (bestOracle != null) 
					eliminateStar();
				
				generateCode();
				log.info("DONE");
			} else {
				log.info("UNSAT");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		System.out.println(stat.toString());
	}

	public boolean runBeforeGenerateCode() throws IOException,
			InterruptedException {
		
	    parseProgram();
        // dump (prog, "After parsing:");
	    processing();
		generateDAG();
		startCEGIS();

		log.fine(stat.toString());
		return bestOracle != null;
	}
	
	public void processing() {
        prog = (Program) prog.accept(new ConstantReplacer(null));
        // dump (prog, "After replacing constants:");
        if (!SemanticChecker.check(prog, isParallel()))
            throw new IllegalStateException("Semantic check failed");

        prog = preprocessProgram(prog); // perform prereq transformations

        if (prog == null)
            throw new IllegalStateException();

        prog = lowering(prog);
	}
	
	public void generateDAG() throws IOException {
	    loop = new CEGISLoop(programName, options, stat, internalRControl());
	    solver = loop.selectBackend(options.smtOpts.backend, 
                "bv".equals(options.smtOpts.intmodel)
                , options.debugOpts.trace, true);

        solver.setIntNumBits(options.bndOpts.intbits);

        vtype = solver.createFormula(
                options.bndOpts.intbits, options.bndOpts.inbits,
                options.bndOpts.cbits, options.smtOpts.theoryOfArray, stat, varGen);

        ProduceSMTCode partialEval = getPartialEvaluator(vtype);
        prog.accept(partialEval);
        vtype.finalize();
        vtype.optimize();
	}

	public void startCEGIS() {
	    
	    loop.start(vtype, solver);
        bestOracle = loop.getSolution();
        stat = loop.getStat();
	}

	protected ProduceSMTCode getPartialEvaluator(NodeToSmtVtype vtype) {
		ProduceSMTCode partialEval = new ProduceSMTCode(vtype, varGen,
		        options.smtOpts.theoryOfArray,
		        options.smtOpts.intmodel == IntModel.bv,
		        options.bndOpts.unrollAmnt,
				internalRControl(), options.debugOpts.trace);
		return partialEval;
	}

	public static void main(String[] args) {
		new SequentialSMTSketchMain(args).run();
		System.exit(0);
	}

}
