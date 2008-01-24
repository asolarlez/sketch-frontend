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

package streamit.frontend;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;

import streamit.frontend.CommandLineParamManager.POpts;
import streamit.frontend.codegenerators.NodesToC;
import streamit.frontend.codegenerators.NodesToCTest;
import streamit.frontend.codegenerators.NodesToH;
import streamit.frontend.experimental.deadCodeElimination.EliminateDeadCode;
import streamit.frontend.experimental.eliminateTransAssign.EliminateTransitiveAssignments;
import streamit.frontend.experimental.preprocessor.FlattenStmtBlocks;
import streamit.frontend.experimental.preprocessor.PreprocessSketch;
import streamit.frontend.experimental.preprocessor.SimplifyVarNames;
import streamit.frontend.experimental.preprocessor.TypeInferenceForStars;
import streamit.frontend.experimental.simplifier.ScalarizeVectorAssignments;
import streamit.frontend.nodes.MakeBodiesBlocks;
import streamit.frontend.nodes.Program;
import streamit.frontend.nodes.TempVarGen;
import streamit.frontend.nodes.Type;
import streamit.frontend.nodes.TypePrimitive;
import streamit.frontend.nodes.TypeStruct;
import streamit.frontend.passes.AssembleInitializers;
import streamit.frontend.passes.BitTypeRemover;
import streamit.frontend.passes.BitVectorPreprocessor;
import streamit.frontend.passes.ConstantReplacer;
import streamit.frontend.passes.DisambiguateUnaries;
import streamit.frontend.passes.EliminateAnyorder;
import streamit.frontend.passes.EliminateArrayRange;
import streamit.frontend.passes.EliminateBitSelector;
import streamit.frontend.passes.EliminateMultiDimArrays;
import streamit.frontend.passes.EliminateNestedArrAcc;
import streamit.frontend.passes.EliminateStructs;
import streamit.frontend.passes.ExtractRightShifts;
import streamit.frontend.passes.ExtractVectorsInCasts;
import streamit.frontend.passes.FunctionParamExtension;
import streamit.frontend.passes.SemanticChecker;
import streamit.frontend.passes.SeparateInitializers;
import streamit.frontend.solvers.SATBackend;
import streamit.frontend.stencilSK.EliminateStarStatic;
import streamit.frontend.stencilSK.SimpleCodePrinter;
import streamit.frontend.stencilSK.StaticHoleTracker;
import streamit.frontend.tosbit.SimplifyExpressions;
import streamit.frontend.tosbit.ValueOracle;
import streamit.frontend.tosbit.recursionCtrl.AdvancedRControl;
import streamit.frontend.tosbit.recursionCtrl.RecursionControl;



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
public class ToSBit
{

	// protected final CommandLineParams params;
	protected Program beforeUnvectorizing=null;

	public static final CommandLineParamManager params = new CommandLineParamManager();


	protected ToSBit(String[] args){
		this.setCommandLineParams();
		params.loadParams(args);
	}



/**
 * This function produces a recursion control that is used by all the user visible transformations.
 * @return
 */
	public RecursionControl visibleRControl(){
		// return new BaseRControl(params.inlineAmt);
		return new AdvancedRControl(params.flagValue("branchamnt"), params.flagValue("inlineamnt"), prog);
	}

	/**
	 * This function produces a recursion control that is used by all transformations that are not user visible. 
	 * In particular, the conversion to boolean. By default it is the same as the visibleRControl.
	 * @return
	 */
	public RecursionControl internalRControl(){
		
		return visibleRControl();
	}


	/**
	 * Generate a Program object that includes built-in structures
	 * and streams with code, but no user code.
	 *
	 * @returns a StreamIt program containing only built-in code
	 */
	public static Program emptyProgram()
	{
		List streams = new java.util.ArrayList();
		List<TypeStruct> structs = new java.util.ArrayList<TypeStruct>();

		// Complex structure type:
		List<String> fields = new java.util.ArrayList<String>();
		List<Type> ftypes = new java.util.ArrayList<Type>();
		Type floattype = TypePrimitive.floattype ;
		fields.add("real");
		ftypes.add(floattype);
		fields.add("imag");
		ftypes.add(floattype);
		//TypeStruct complexStruct =
		//	new TypeStruct(null, "Complex", fields, ftypes);
		//structs.add(complexStruct);

		return new Program(null, streams, structs);
	}

	/**
	 * Read, parse, and combine all of the StreamIt code in a list of
	 * files.  Reads each of the files in <code>inputFiles</code> in
	 * turn and runs <code>streamit.frontend.StreamItParserFE</code>
	 * over it.  This produces a
	 * <code>streamit.frontend.nodes.Program</code> containing lists
	 * of structures and streams; combine these into a single
	 * <code>streamit.frontend.nodes.Program</code> with all of the
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
	public Program parseFiles(List inputFiles)
	throws java.io.IOException, antlr.RecognitionException, antlr.TokenStreamException
	{
		Program prog = emptyProgram();
		for (Iterator iter = inputFiles.iterator(); iter.hasNext(); )
		{
			String fileName = (String)iter.next();
			InputStream inStream = new FileInputStream(fileName);
			DataInputStream dis = new DataInputStream(inStream);
			StreamItLex lexer = new StreamItLex(dis);
			StreamItParserFE parser = new StreamItParserFE(lexer);
			parser.setFilename(fileName);
			Program pprog = parser.program();
			if(pprog==null) return null;
			List newStreams, newStructs;
			newStreams = new java.util.ArrayList();
			newStreams.addAll(prog.getStreams());
			newStreams.addAll(pprog.getStreams());
			newStructs = new java.util.ArrayList();
			newStructs.addAll(prog.getStructs());
			newStructs.addAll(pprog.getStructs());
			prog = new Program(null, newStreams, newStructs);
		}
		return prog;
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
		
		prog = (Program)prog.accept(new MakeBodiesBlocks());
		//dump (prog, "MBB:");
		prog = (Program)prog.accept(new EliminateStructs(varGen));
		prog = (Program)prog.accept(new DisambiguateUnaries(varGen));
		//dump (prog, "After eliminating structs:");
		prog = (Program)prog.accept(new EliminateMultiDimArrays());
		//dump (prog, "After second elimination of multi-dim arrays:");
		prog = (Program)prog.accept(new ExtractRightShifts(varGen));
		prog = (Program)prog.accept(new ExtractVectorsInCasts(varGen));
		prog = (Program)prog.accept(new SeparateInitializers());
		//dump (prog, "SeparateInitializers:");
		//prog = (Program)prog.accept(new NoRefTypes());
		prog = (Program)prog.accept(new ScalarizeVectorAssignments(varGen, true));

		if( params.hasFlag("showpartial")  ) prog.accept(new SimpleCodePrinter());

		prog = (Program)prog.accept(new EliminateNestedArrAcc());
		//dump (prog, "After lowerIR:");
	}


	TempVarGen varGen = new TempVarGen();
	Program prog = null;
	ValueOracle oracle;
	Program finalCode;

	public Program parseProgram(){
		try
		{
			prog = parseFiles(params.inputFiles);
		}
		catch (Exception e)
		{
			//e.printStackTrace(System.err);
			throw new RuntimeException(e);
		}

		if (prog == null)
		{
			System.err.println("Compilation didn't generate a parse tree.");
			throw new IllegalStateException();
		}
		return prog;

	}

	protected Program preprocessProgram(Program lprog) {
		//invoke post-parse passes

		//dump (prog, "before:");
		// prog = (Program)prog.accept(new NoRefTypes());
		//dump (prog, "bef fpe:");
		lprog = (Program)lprog.accept(new EliminateAnyorder(varGen));
		lprog = (Program)lprog.accept(new FunctionParamExtension(true));		
		//dump (prog, "fpe:");
		lprog = (Program)lprog.accept(new DisambiguateUnaries(varGen));
		lprog = (Program)lprog.accept(new TypeInferenceForStars());
		//dump (prog, "tifs:");
		lprog = (Program) lprog.accept (new EliminateMultiDimArrays ());
		//dump (prog, "After first elimination of multi-dim arrays:");
		lprog = (Program) lprog.accept( new PreprocessSketch( varGen, params.flagValue("unrollamnt"), visibleRControl() ) );
		//dump (prog, "aftpp");
		return lprog;
	}

	public boolean partialEvalAndSolve(){
		lowerIRToJava();
		SATBackend solver = new SATBackend(params, internalRControl(), varGen);
		boolean tmp = solver.partialEvalAndSolve(prog);
		oracle =solver.getOracle();
		return tmp;
	}

	public void eliminateStar(){
		finalCode=(Program)beforeUnvectorizing.accept(new EliminateStarStatic(oracle));
		dump(finalCode, "after elim star");
		finalCode=(Program)finalCode.accept(new PreprocessSketch( varGen, params.flagValue("unrollamnt"), visibleRControl(), true ));
		dump(finalCode, "after postproc");
		finalCode = (Program)finalCode.accept(new FlattenStmtBlocks());
		dump(finalCode, "after flattening");
		finalCode = (Program)finalCode.accept(new EliminateTransitiveAssignments());
		//System.out.println("=========  After ElimTransAssign  =========");
		// dump(finalCode, "Before DCE");
		finalCode = (Program)finalCode.accept(new EliminateDeadCode());
		// dump(finalCode, "after DCE");
		//System.out.println("=========  After ElimDeadCode  =========");
		//finalCode.accept( new SimpleCodePrinter() );
		finalCode = (Program)finalCode.accept(new SimplifyVarNames());
		finalCode = (Program)finalCode.accept(new AssembleInitializers());
		/*
    	 finalCode =
             (Program) beforeUnvectorizing.accept (
                 new EliminateStar(oracle, params.unrollAmt, newRControl(),3));
         finalCode =
             (Program) finalCode.accept (
                 new EliminateStar(oracle, params.unrollAmt, newRControl(), 3));
		 */
	}

	protected String getOutputFileName() {
		String resultFile = params.sValue("outputprogname");
		if(resultFile==null) {
			resultFile=params.inputFiles.get(0);
		}
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


		String resultFile = getOutputFileName();
		String hcode = (String)finalCode.accept(new NodesToH(resultFile));
		String ccode = (String)finalCode.accept(new NodesToC(varGen,resultFile));
		if(!params.hasFlag("outputcode")){
			//finalCode.accept( new SimpleCodePrinter() );
			//System.out.println(hcode);
			System.out.println(ccode);
		}else{
			try{
				{
					Writer outWriter = new FileWriter(params.sValue("outputdir") +resultFile+".h");
					outWriter.write(hcode);
					outWriter.flush();
					outWriter.close();
					outWriter = new FileWriter(params.sValue("outputdir")+resultFile+".cpp");
					outWriter.write(ccode);
					outWriter.flush();
					outWriter.close();
				}
				if( params.hasFlag("outputtest")  ) {
					String testcode=(String)beforeUnvectorizing.accept(new NodesToCTest(resultFile));
					Writer outWriter = new FileWriter(params.sValue("outputdir")+resultFile+"_test.cpp");
					outWriter.write(testcode);
					outWriter.flush();
					outWriter.close();
				}
				if( params.hasFlag("outputtest") ) {
					Writer outWriter = new FileWriter(params.sValue("outputdir")+"script");
					outWriter.write("#!/bin/sh\n");

					outWriter.write("g++ -I \"$FRONTEND/include\" -o "+resultFile+" "+resultFile+".cpp "+resultFile+"_test.cpp\n");

					outWriter.write("./"+resultFile+"\n");
					outWriter.flush();
					outWriter.close();
				}
			}
			catch (java.io.IOException e){
				throw new RuntimeException(e);
			}
		}
	}



	protected void setCommandLineParams(){

		params.setAllowedParam("D", new POpts(POpts.VVAL,
				"--D VAR val    \t If the program contains a global variable VAR, it sets its value to val.",
				null, null));

		params.setAllowedParam("unrollamnt", new POpts(POpts.NUMBER,
				"--unrollamnt n \t It sets the unroll ammount for loops to n.",
				"8", null) );

		params.setAllowedParam("inlineamnt", new POpts(POpts.NUMBER,
				"--inlineamnt n \t Bounds inlining to n levels of recursion, so" +
				"\n\t\t each function can appear at most n times in the stack.",
				"5", null) );

		params.setAllowedParam("branchamnt", new POpts(POpts.NUMBER,
				"--branchamnt n \t This flag is also used for recursion control. " +
				"\n\t\t It bounds inlining based on the idea that if a function calls " +
				"\n\t\t itself recureively ten times, we want to inline it less than a function" +
				"\n\t\t that calls itself recursively only once. In this case, n is the " +
				"\n\t\t maximum value of the branching factor, which is the number of times" +
				"\n\t\t a function calls itself recursively, times the amount of inlining. ",
				"15", null) );

		params.setAllowedParam("incremental", new POpts(POpts.NUMBER,
				"--incremental n\t Tells the solver to incrementally grow the size of integer holes from 1 to n bits.",
				"5", null) );

		params.setAllowedParam("timeout", new POpts(POpts.NUMBER,
				"--timeout min  \t Kills the solver after min minutes.",
				null, null) );

		params.setAllowedParam("fakesolver", new POpts(POpts.FLAG,
				"--fakesolver   \t This flag indicates that the SAT solver should not be invoked. " +
				"\n \t\t Instead the frontend should look for a solution file, and generate the code from that. " +
				"\n \t\t It is useful when working with sketches that take a long time to resolve" +
				"\n \t\t if one wants to play with different settings for code generation.",
				null, null) );

		params.setAllowedParam("seed", new POpts(POpts.NUMBER,
				"--seed s       \t Seeds the random number generator with s.",
				null, null) );

		params.setAllowedParam("outputcode", new POpts(POpts.FLAG,
				"--outputcode   \t Use this flag if you want the compiler to produce C code.",
				null, null) );

		params.setAllowedParam("outputtest", new POpts(POpts.FLAG,
				"--outputtest   \t Produce also a harness to test the generated C code.",
				null, null) );

		params.setAllowedParam("outputdir", new POpts(POpts.STRING,
				"--outputdir dir\t Set the directory where you want the generated code to live.",
				"./", null) );

		params.setAllowedParam("outputprogname", new POpts(POpts.STRING,
				"--outputprogname name \t Set the name of the output C files." +
				"\n \t\t By default it is the name of the first input file.",
				null, null) );


		params.setAllowedParam("output", new POpts(POpts.STRING,
				"--output file  \t Temporary output file used to communicate with backend solver. " +
				"\n \t\t This flag is already set by the sketch script, so don't try to set it yourself.",
				null, null) );

		params.setAllowedParam("sbitpath", new POpts(POpts.STRING,
				"--sbitpath path\t Path where the SBitII solver can be found. This flag " +
				"\n \t\t is already set by the sketch script, so don't try to set it yourself.",
				null, null) );

		params.setAllowedParam("showpartial", new POpts(POpts.FLAG,
				"--showpartial  \t Show the preprocessed sketch before it is sent to the solver.",
				null, null) );
		
		params.setAllowedParam("forcecodegen", new POpts(POpts.FLAG,
				"--forcecodegen  \t Forces code generation. Even if the sketch fails to resolve, " +
				"                \t this flag will force the synthesizer to produce code from the latest known control values.",
				null, null) );

		params.setAllowedParam("keeptmpfiles", new POpts(POpts.FLAG,
				"--keeptmpfiles  \t Keep intermediate files. Useful for debugging the compiler.",
				null, null) );

		params.setAllowedParam("cbits", new POpts(POpts.NUMBER,
				"--cbits n      \t Specify the number of bits to use for integer holes.",
				"5", null) );

		params.setAllowedParam("inbits", new POpts(POpts.NUMBER,
				"--inbits n      \t Specify the number of bits to use for integer inputs.",
				"5", null) );

	}


	protected Program doBackendPasses(Program prog) {
		if( false && params.hasFlag("outputcode") ) {
			prog=(Program) prog.accept(new AssembleInitializers());
			prog=(Program) prog.accept(new BitVectorPreprocessor(varGen));
			//prog.accept(new SimpleCodePrinter());
			prog=(Program) prog.accept(new BitTypeRemover(varGen));
			prog=(Program) prog.accept(new SimplifyExpressions());
		}
		return prog;
	}

	public void generateCode(){
		finalCode=doBackendPasses(finalCode);
		outputCCode();
	}

	public void run()
	{
		parseProgram();

		prog = (Program)prog.accept(new ConstantReplacer(params.varValues("D")));
		//dump (prog, "After replacing constants:");
		if (!SemanticChecker.check(prog))
			throw new IllegalStateException("Semantic check failed");

		prog=preprocessProgram(prog); // perform prereq transformations
		//prog.accept(new SimpleCodePrinter());
		// RenameBitVars is buggy!! prog = (Program)prog.accept(new RenameBitVars());
		// if (!SemanticChecker.check(prog))
		//	throw new IllegalStateException("Semantic check failed");
		
		if (prog == null)
			throw new IllegalStateException();

		oracle = new ValueOracle( new StaticHoleTracker(varGen)/* new SequentialHoleTracker(varGen) */);
		partialEvalAndSolve();
		eliminateStar();

		generateCode();
		System.out.println("DONE");

	}
	
	
	protected void backendParameters(List<String> commandLineOptions){
		if( params.hasFlag("inbits") ){
			commandLineOptions.add("-overrideInputs");
			commandLineOptions.add( "" + params.flagValue("inbits") );
		}
		if( params.hasFlag("seed") ){
			commandLineOptions.add("-seed");
			commandLineOptions.add( "" + params.flagValue("seed") );
		}
	}
	



	String solverErrorStr;



	protected void dump (Program prog) {
		dump (prog, "");
	}
	protected void dump (Program prog, String message) {
		System.out.println("=============================================================");
		System.out.println ("  ----- "+ message +" -----");
		prog.accept( new SimpleCodePrinter() );
		System.out.println("=============================================================");
	}

	public static void main(String[] args)
	{
		new ToSBit(args).run();
		System.exit(0);
	}



}

