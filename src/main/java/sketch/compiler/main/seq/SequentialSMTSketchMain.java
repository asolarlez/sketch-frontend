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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import sketch.compiler.CommandLineParamManager;
import sketch.compiler.Directive;
import sketch.compiler.CommandLineParamManager.POpts;
import sketch.compiler.Directive.OptionsDirective;
import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.StreamSpec;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.ExprStar;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.ast.core.typs.TypeStruct;
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
import sketch.compiler.smt.ProduceSMTCode;
import sketch.compiler.smt.SMTBackend;
import sketch.compiler.smt.CEGISLoop.CEGISStat;
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
public class SequentialSMTSketchMain {
    
	// protected final CommandLineParams params;
	protected Program beforeUnvectorizing = null;
	Program finalCode;
	Program prog = null;
	
	protected String programName;
	
	public static final CommandLineParamManager params = CommandLineParamManager
			.getParams();
	private static Logger log = Logger.getLogger(SequentialSMTSketchMain.class.getCanonicalName());
	
	protected TempVarGen varGen = new TempVarGen("__sa");
	
	SmtValueOracle bestOracle;
	
	CEGISStat stat;
	
	/*
	 * Getters & Setters
	 */
	public boolean isParallel() {
		return false;
	}

	public AbstractValueOracle getOracle() {
		return this.bestOracle;
	}
	
	public CEGISStat getSolutionStat() {
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
		this.setCommandLineParams();
		
		params.loadParams(args);
		
		Logger rootLogger = Logger.getLogger("");
		int verbosity = params.flagValue("verbosity");
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
	}
	
	protected SequentialSMTSketchMain() {}
	
	public static RecursionControl visibleRControl(Program p) {
		// return new BaseRControl(params.inlineAmt);
		return new AdvancedRControl(params.flagValue("branchamnt"), params
				.flagValue("inlineamnt"), p);
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
		List<String> cppDefs = params.listValue("def");
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
		prog = (Program) prog.accept(new EliminateStructs(varGen, params
				.flagValue("heapsize")));
		prog = (Program) prog.accept(new ReplaceStructTypeWithInt());
		
		prog = (Program) prog.accept(new DisambiguateUnaries(varGen));
//		dump (prog, "After eliminating structs:");
		prog = (Program) prog.accept(new EliminateMultiDimArrays());
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
		if ("assertions".equals(params.sValue("arrayOOBPolicy"))) {
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
		
		if (params.flagEquals("showphase", "lowering"))
			dump(prog, "Lowering the code previous to Symbolic execution.");

		prog = (Program) prog.accept(new EliminateNestedArrAcc());
//		 dump (prog, "After lowerIR:");
		return prog;
	}

	public Program parseProgram() {
		
		try {
			Pair<Program, Set<Directive>> res = parseFiles(params.inputFiles);
			prog = res.getFirst();
			processDirectives(res.getSecond());
			
			if (params.flagEquals("showphase", "parse"))
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

	protected void processDirectives(Set<Directive> D) {
		for (Directive d : D)
			if (d instanceof OptionsDirective)
				params.loadParams(((OptionsDirective) d).options());
	}

	protected Program preprocessProgram(Program lprog) {
		boolean useInsertEncoding = params.flagEquals("reorderEncoding",
				"exponential");
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
		
		lprog = (Program) lprog.accept(new EliminateMultiDimArrays());
//		dump (lprog, "After first elimination of multi-dim arrays:");
		lprog = (Program) lprog.accept(new PreprocessSketch(varGen, params
				.flagValue("unrollamnt"), visibleRControl(lprog)));
		if (params.flagEquals("showphase", "preproc"))
			dump(lprog, "After Preprocessing");
//		dump(lprog, "After Preprocessing");
		return lprog;
	}

	public void eliminateStar(){
		finalCode=(Program)beforeUnvectorizing.accept(new EliminateStarStatic(bestOracle));
//		 dump(finalCode, "after elim star");
		finalCode=(Program)finalCode.accept(new PreprocessSketch( varGen, params.flagValue("unrollamnt"), visibleRControl(), true ));
		// dump(finalCode, "After partially evaluating generated code.");
		finalCode = (Program)finalCode.accept(new FlattenStmtBlocks());
		if(params.flagEquals("showphase", "postproc")) 
			dump(finalCode, "After Flattening.");
		finalCode = (Program)finalCode.accept(new EliminateTransAssns());
		//System.out.println("=========  After ElimTransAssign  =========");
		if(params.flagEquals("showphase", "taelim")) 
			dump(finalCode, "After Eliminating transitive assignments.");
		finalCode = (Program)finalCode.accept(new EliminateDeadCode(params.hasFlag("keepasserts")));
		//dump(finalCode, "After Dead Code elimination.");
		//System.out.println("=========  After ElimDeadCode  =========");
		finalCode = (Program)finalCode.accept(new SimplifyVarNames());
		finalCode = (Program)finalCode.accept(new AssembleInitializers());
		
		finalCode = (Program)finalCode.accept(new ArithmeticSimplification());
		if(params.flagEquals("showphase", "final")) 
			dump(finalCode, "After Dead Code elimination.");
	}

	protected String getOutputFileName() {
		String resultFile = params.sValue("outputprogname");
		if (resultFile == null) {
			resultFile = params.inputFiles.get(0);
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
		
		if (!params.hasFlag("outputcode")) {
			 finalCode.accept( new SimpleCodePrinter() );
			// System.out.println(hcode);
//			System.out.println(ccode);
		} else {
			try {
				{
					String hcode = (String) finalCode.accept(new NodesToH(resultFile));
					String ccode = (String) finalCode.accept(new NodesToC(varGen,
							resultFile));
					
					Writer outWriter = new FileWriter(params
							.sValue("outputdir")
							+ resultFile + ".h");
					outWriter.write(hcode);
					outWriter.flush();
					outWriter.close();
					outWriter = new FileWriter(params.sValue("outputdir")
							+ resultFile + ".cpp");
					outWriter.write(ccode);
					outWriter.flush();
					outWriter.close();
				}
				if (params.hasFlag("outputtest")) {
					String testcode = (String) beforeUnvectorizing
							.accept(new NodesToCTest(resultFile));
					Writer outWriter = new FileWriter(params
							.sValue("outputdir")
							+ resultFile + "_test.cpp");
					outWriter.write(testcode);
					outWriter.flush();
					outWriter.close();
				}
				if (params.hasFlag("outputtest")) {
					Writer outWriter = new FileWriter(params
							.sValue("outputdir")
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

	protected void setCommandLineParams() {

		params
				.setAllowedParam(
						"D",
						new POpts(
								POpts.VVAL,
								"--D VAR val    \t If the program contains a global variable VAR, it sets its value to val.",
								null, null));

		params.setAllowedParam("unrollamnt", new POpts(POpts.NUMBER,
				"--unrollamnt n \t It sets the unroll ammount for loops to n.",
				"8", null));

		params
				.setAllowedParam(
						"inlineamnt",
						new POpts(
								POpts.NUMBER,
								"--inlineamnt n \t Bounds inlining to n levels of recursion, so"
										+ "\n\t\t each function can appear at most n times in the stack.",
								"5", null));

		params
				.setAllowedParam(
						"heapsize",
						new POpts(
								POpts.NUMBER,
								"--heapsize n \t Size of the heap for each object. This is the maximum"
										+ "\n\t\t number of objects of a given type that the program may allocate.",
								"11", null));

		params
				.setAllowedParam(
						"branchamnt",
						new POpts(
								POpts.NUMBER,
								"--branchamnt n \t This flag is also used for recursion control. "
										+ "\n\t\t It bounds inlining based on the idea that if a function calls "
										+ "\n\t\t itself recureively ten times, we want to inline it less than a function"
										+ "\n\t\t that calls itself recursively only once. In this case, n is the "
										+ "\n\t\t maximum value of the branching factor, which is the number of times"
										+ "\n\t\t a function calls itself recursively, times the amount of inlining. ",
								"15", null));

		params
				.setAllowedParam(
						"incremental",
						new POpts(
								POpts.NUMBER,
								"--incremental n\t Tells the solver to incrementally grow the size of integer holes from 1 to n bits.",
								"5", null));

		params.setAllowedParam("timeout", new POpts(POpts.NUMBER,
				"--timeout min  \t Kills the solver after min minutes.", "0",
				null));

		params
				.setAllowedParam(
						"fakesolver",
						new POpts(
								POpts.FLAG,
								"--fakesolver   \t This flag indicates that the SAT solver should not be invoked. "
										+ "\n \t\t Instead the frontend should look for a solution file, and generate the code from that. "
										+ "\n \t\t It is useful when working with sketches that take a long time to resolve"
										+ "\n \t\t if one wants to play with different settings for code generation.",
								null, null));

		params.setAllowedParam("theoryofarray", new POpts(POpts.FLAG,
				"--theoryofarray\t Uses theory of array", null, null));
		
		params.setAllowedParam("funchash", new POpts(POpts.FLAG,
                "--funchash\t Function hashing", null, null));
		
		params.setAllowedParam("canon", new POpts(POpts.FLAG,
                "--canon\t Canonicalize arithmetics", null, null));
		
		params.setAllowedParam("linear", new POpts(POpts.FLAG,
                "--linear\t Linearize arithmetics", null, null));
		
		params.setAllowedParam("uselet", new POpts(POpts.FLAG,
                "--uselet\t Use LET construct", null, null));
		
		
		params.setAllowedParam("bv", new POpts(POpts.FLAG,
				"--bv\t Uses BitVector in the given backend", null, null));
		
		params.setAllowedParam("seed", new POpts(POpts.NUMBER,
				"--seed s       \t Seeds the random number generator with s.",
				null, null));

		params
				.setAllowedParam(
						"verbosity",
						new POpts(
								POpts.NUMBER,
								"--verbosity n       \t Sets the level of verbosity for the output. 0 is quite mode 5 is the most verbose.",
								"3", null));

		params
				.setAllowedParam(
						"cex",
						new POpts(
								POpts.FLAG,
								"--cex       \t Show the counterexample inputs produced by the solver (Equivalend to backend flag -showinputs).",
								null, null));

		params
				.setAllowedParam(
						"outputcode",
						new POpts(
								POpts.FLAG,
								"--outputcode   \t Use this flag if you want the compiler to produce C code.",
								null, null));

		params
				.setAllowedParam(
						"keepasserts",
						new POpts(
								POpts.FLAG,
								"--keepasserts   \t The synthesizer guarantees that all asserts will succeed."
										+ "\n \t\t For this reason, all asserts are removed from generated code by default. However, "
										+ "\n \t\t sometimes it is useful for debugging purposes to keep the assertions around.",
								null, null));

		params
				.setAllowedParam(
						"outputtest",
						new POpts(
								POpts.FLAG,
								"--outputtest   \t Produce also a harness to test the generated C code.",
								null, null));

		params
				.setAllowedParam(
						"outputdir",
						new POpts(
								POpts.STRING,
								"--outputdir dir\t Set the directory where you want the generated code to live.",
								"./", null));
		
		params
		.setAllowedParam(
				"tmpdir",
				new POpts(
						POpts.STRING,
						"--tmpdir dir\t Set the directory where you want the temporary files to live.",
						"/tmp", null));

		params
				.setAllowedParam(
						"outputprogname",
						new POpts(
								POpts.STRING,
								"--outputprogname name \t Set the name of the output C files."
										+ "\n \t\t By default it is the name of the first input file.",
								null, null));

		params
				.setAllowedParam(
						"smtpath",
						new POpts(
								POpts.STRING,
								"--smtpath path\t Path to the SMT solver executable. default to cvc3",
								"cvc3", null));

		params
				.setAllowedParam(
						"backend",
						new POpts(
								POpts.STRING,
								"--backend cvc3|cvc3smtlib\t Choose the backend of the SMT code",
								"cvc3", null));

		params
				.setAllowedParam(
						"forcecodegen",
						new POpts(
								POpts.FLAG,
								"--forcecodegen  \t Forces code generation. Even if the sketch fails to resolve, "
										+ "                \t this flag will force the synthesizer to produce code from the latest known control values.",
								null, null));

		params
				.setAllowedParam(
						"keeptmpfiles",
						new POpts(
								POpts.FLAG,
								"--keeptmpfiles  \t Keep intermediate files. Useful for debugging the compiler.",
								null, null));

		params
				.setAllowedParam(
						"cbits",
						new POpts(
								POpts.NUMBER,
								"--cbits n      \t Specify the number of bits to use for integer holes.",
								"5", null));

		params
				.setAllowedParam(
						"inbits",
						new POpts(
								POpts.NUMBER,
								"--inbits n      \t Specify the number of bits to use for integer inputs.",
								"5", null));
		
		params
		.setAllowedParam(
				"intbits",
				new POpts(
						POpts.NUMBER,
						"--intbits n      \t Specify the number of bits to use for integers.",
						"32", null));

		params
				.setAllowedParam(
						"trace",
						new POpts(
								POpts.FLAG,
								"--trace  \t Show a trace of the symbolic execution. Useful for debugging purposes.",
								null, null));
		params
				.setAllowedParam(
						"reorderEncoding",
						new POpts(
								POpts.STRING,
								"--reorderEncoding  which \t How reorder blocks should be rewritten.  Current supported:\n"
										+ "             \t * exponential -- use 'insert' blocks\n"
										+ "             \t * quadratic -- use a loop of switch statements\n",
								"exponential", null));

		params
				.setAllowedParam(
						"def",
						new POpts(
								POpts.MULTISTRING,
								"--def        \t Vars to define for the C preprocessor.\n"
										+ "             \t Consider also using the 'safer' option --D VAR value\n"
										+ "             \t Example use:  '--def _FOO=1 --def _BAR=false ...'",
								null, null));

		Map<String, String> phases = new HashMap<String, String>();
		phases.put("parse", "After parsing");
		phases.put("preproc", " After preprocessing.");
		phases.put("lowering", " Previous to Symbolic execution.");
		phases.put("postproc",
				" After partially evaluating the generated code (ugly).");
		phases
				.put("taelim",
						" After eliminating transitive assignments (before cse, ugly).");
		phases.put("final", " After all optimizations.");
		params
				.setAllowedParam(
						"showphase",
						new POpts(
								POpts.TOKEN,
								"--showphase OPT\t Show the partially evaluated code after the indicated phase of pre or post processing.",
								"5", phases));

		Map<String, String> solvers = new HashMap<String, String>();
		solvers.put("MINI", "MiniSat solver");
		solvers.put("ABC", "ABC solver");
		params.setAllowedParam("synth", new POpts(POpts.TOKEN,
				"--synth OPT\t SAT solver to use for synthesis.", "MINI",
				solvers));
		params.setAllowedParam("verif", new POpts(POpts.TOKEN,
				"--verif OPT\t SAT solver to use for verification.", "MINI",
				solvers));

		Map<String, String> failurePolicies = new HashMap<String, String>();
		failurePolicies.put("wrsilent_rdzero",
				"Read a zero, silently ignore writes");
		failurePolicies.put("assertions",
				"Fail assertions for reads and writes");
		params.setAllowedParam("arrayOOBPolicy", new POpts(POpts.TOKEN,
				"--arrayOOBPolicy policy \t What to do when an array access would be out\n"
						+ "                        \t of bounds.",
				"wrsilent_rdzero", failurePolicies));
	}

	protected Program doBackendPasses(Program prog) {
		if (params.hasFlag("outputcode")) {
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

		prog = (Program) prog
				.accept(new ConstantReplacer(params.varValues("D")));
		// dump (prog, "After replacing constants:");
		if (!SemanticChecker.check(prog, isParallel()))
			throw new IllegalStateException("Semantic check failed");

		prog = preprocessProgram(prog); // perform prereq transformations
		// prog.accept(new SimpleCodePrinter());
		// RenameBitVars is buggy!! prog = (Program)prog.accept(new
		// RenameBitVars());
		// if (!SemanticChecker.check(prog))
		// throw new IllegalStateException("Semantic check failed");

		if (prog == null)
			throw new IllegalStateException();

		prog = lowering(prog);

		CEGISLoop loop = startCEGIS();
		stat = loop.getStat();

		return bestOracle != null;
	}

	private CEGISLoop startCEGIS() throws IOException {
		CEGISLoop loop = new CEGISLoop(programName, params, internalRControl());

		SMTBackend solver = loop.selectBackend(params.sValue("backend"), params
				.hasFlag("bv"), params.hasFlag("trace"), true);

		solver.setIntNumBits(params.flagValue("intbits"));

		// Toolbox.pause();

		NodeToSmtVtype vtype = solver.createFormula(
				params.flagValue("intbits"), params.flagValue("inbits"), params
						.flagValue("cbits"), params.hasFlag("theoryofarray"), varGen);

		ProduceSMTCode partialEval = getPartialEvaluator(vtype);
		prog.accept(partialEval);
		vtype.finalize();
		
//		 Toolbox.pause("Done generating DAG");

		vtype.optimize();

//		 Toolbox.pause("Done Optimizing DAG");
		loop.start(vtype, solver);

		bestOracle = loop.getSolution();
		return loop;
	}

	protected ProduceSMTCode getPartialEvaluator(NodeToSmtVtype vtype) {
		ProduceSMTCode partialEval = new ProduceSMTCode(vtype, varGen,
				params.flagValue("unrollamnt"), internalRControl(), params
						.hasFlag("trace"));
		return partialEval;
	}


	protected void backendParameters(List<String> commandLineOptions) {
		if (params.hasFlag("inbits")) {
			commandLineOptions.add("-overrideInputs");
			commandLineOptions.add("" + params.flagValue("inbits"));
		}
		if (params.hasFlag("seed")) {
			commandLineOptions.add("-seed");
			commandLineOptions.add("" + params.flagValue("seed"));
		}
		if (params.hasFlag("cex")) {
			commandLineOptions.add("-showinputs");
		}
		if (params.hasFlag("verbosity")) {
			commandLineOptions.add("-verbosity");
			commandLineOptions.add("" + params.flagValue("verbosity"));
		}
		if (params.hasFlag("synth")) {
			commandLineOptions.add("-synth");
			commandLineOptions.add("" + params.sValue("synth"));
		}
		if (params.hasFlag("verif")) {
			commandLineOptions.add("-verif");
			commandLineOptions.add("" + params.sValue("verif"));
		}
	}

	String solverErrorStr;
	
	/*
	 * Helper functions
	 */
	public static void dump(Program prog) {
		dump(prog, "");
	}

	public static void dump(Program prog, String message) {
		System.out
				.println("=============================================================");
		System.out.println("  ----- " + message + " -----");
		prog.accept(new SimpleCodePrinter());
		System.out
				.println("=============================================================");
	}

	public static void main(String[] args) {
		new SequentialSMTSketchMain(args).run();
		System.exit(0);
	}

}