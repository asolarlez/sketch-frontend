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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import streamit.frontend.nodes.MakeBodiesBlocks;
import streamit.frontend.nodes.Program;
import streamit.frontend.nodes.TempVarGen;
import streamit.frontend.nodes.Type;
import streamit.frontend.nodes.TypePrimitive;
import streamit.frontend.nodes.TypeStruct;
import streamit.frontend.passes.AssembleInitializers;
import streamit.frontend.passes.AssignLoopTypes;
import streamit.frontend.passes.BitTypeRemover;
import streamit.frontend.passes.BitVectorPreprocessor;
import streamit.frontend.passes.ConstantReplacer;
import streamit.frontend.passes.DisambiguateUnaries;
import streamit.frontend.passes.EliminateArrayRange;
import streamit.frontend.passes.ExtractRightShifts;
import streamit.frontend.passes.ExtractVectorsInCasts;
import streamit.frontend.passes.FindFreeVariables;
import streamit.frontend.passes.FunctionParamExtension;
import streamit.frontend.passes.GenerateCopies;
import streamit.frontend.passes.NoRefTypes;
import streamit.frontend.passes.NoticePhasedFilters;
import streamit.frontend.passes.SemanticChecker;
import streamit.frontend.passes.SeparateInitializers;
import streamit.frontend.passes.TrimDumbDeadCode;
import streamit.frontend.stencilSK.SimpleCodePrinter;
import streamit.frontend.tojava.ComplexToStruct;
import streamit.frontend.tojava.DoComplexProp;
import streamit.frontend.tojava.EnqueueToFunction;
import streamit.frontend.tojava.InsertIODecls;
import streamit.frontend.tojava.MoveStreamParameters;
import streamit.frontend.tojava.NameAnonymousFunctions;
import streamit.frontend.tosbit.EliminateStar;
import streamit.frontend.tosbit.NodesToC;
import streamit.frontend.tosbit.NodesToCTest;
import streamit.frontend.tosbit.NodesToH;
import streamit.frontend.tosbit.ProduceBooleanFunctions;
import streamit.frontend.tosbit.SequentialHoleTracker;
import streamit.frontend.tosbit.SimplifyExpressions;
import streamit.frontend.tosbit.ValueOracle;


class CommandLineParams{
	boolean printHelp = false;
	boolean libraryFormat = false;
	String outputFile = null;
	String sbitPath = null;
	List<String> inputFiles = new java.util.ArrayList<String>();
	Map<String, Integer> defines=new HashMap<String, Integer>();
	int unrollAmt = 8;
	int inlineAmt = 5;
	boolean incremental = false;
	int incermentalAmt = 0;
	boolean hasTimeout = false;
	int timeout = 30;
	int seed = -1;
	Vector<String> commandLineOptions = new Vector<String>();
	String resultFile = null;	
	boolean doVectorization=false;
	boolean outputFortran=false;
	boolean outputCFiles=false;
	String outputCDir="./";
	boolean outputScript=false;
	boolean outputTest=false;
	boolean fakeSolver=false;
	
	public CommandLineParams(String[] args)
    {
        for (int i = 0; i < args.length; i++)
        {
            if (args[i].equals("--full"))
                ; // Accept but ignore for compatibil5ity
            else if (args[i].equals("--help"))
                printHelp = true;
            else if (args[i].equals("--"))
            {
                // Add all of the remaining args as input files.
                for (i++; i < args.length; i++)
                    inputFiles.add(args[i]);
            }
            else if (args[i].equals("--output"))
                outputFile = args[++i];
            else if (args[i].equals("--ccode"))
            	resultFile = args[++i];
            else if (args[i].equals("--library"))
                libraryFormat = true;
            else if (args[i].equals("--sbitpath"))
                sbitPath = args[++i];
            else if (args[i].equals("-D")) {
                String word = args[++i];
                Integer value = new Integer(args[++i]);
                defines.put(word,value);
            } else if (args[i].equals("--unrollamnt")) {
                Integer value = new Integer(args[++i]);
                unrollAmt = value.intValue(); 
            } else if (args[i].equals("--inlineamnt")) {
                Integer value = new Integer(args[++i]);
                inlineAmt = value.intValue(); 
            }else if (args[i].equals("--incremental")) {
                Integer value = new Integer(args[++i]);
                incremental = true;
                incermentalAmt = value.intValue();
            }else if (args[i].equals("--timeout")) {
                Integer value = new Integer(args[++i]);
                hasTimeout = true;
                timeout = value.intValue();
            }else if (args[i].equals("--seed")) {
                Integer value = new Integer(args[++i]);
                seed = value.intValue();
                commandLineOptions.add("-seed");
                commandLineOptions.add("" + seed);
            }else if (args[i].equals("--dovectorization")) {
            	doVectorization=true;
            }else if (args[i].equals("--outputcfiles")) {
            	outputCFiles=true;
            }else if (args[i].equals("--outputfortran")) {
            	outputFortran=true;
            }else if (args[i].equals("--outputdir")) {
                outputCDir = args[++i];
                if(!outputCDir.endsWith("/"))
                	outputCDir=outputCDir+"/";
            }else if (args[i].equals("--outputscript")) {
            	outputScript=true;
            }else if (args[i].equals("--outputtest")) {
            	outputTest=true;
            }else if (args[i].equals("--fakesolver")) {
            	fakeSolver=true;
            }else if( args[i].charAt(0)=='-') {
            	commandLineOptions.add(args[i]);
                commandLineOptions.add(args[++i]);
            }
            else
                // Maybe check for unrecognized options.
                inputFiles.add(args[i]);
        }
    }

}






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
	
	
	CommandLineParams params;
	Program beforeUnvectorizing=null;
	
	
	ToSBit(CommandLineParams params){
		this.params = params;
	}
	
    public void printUsage()
    {
        System.err.println(
"streamit.frontend.ToJava: StreamIt syntax translator\n" +
"Usage: java streamit.frontend.ToJava [--output out.java] in.str ...\n" +
"\n" +
"Options:\n" +
"  --library      Output code suitable for the Java library\n" +
"  --help         Print this message\n" +
"  --output file  Write output to file, not stdout\n" +
"\n");
    }


    
    private static class NullStream extends OutputStream 
    {
		public void flush() throws IOException {}
		public void close() throws IOException {}
		public void write(int arg0) throws IOException {}
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
        Type floattype = new TypePrimitive(TypePrimitive.TYPE_FLOAT);
        fields.add("real");
        ftypes.add(floattype);
        fields.add("imag");
        ftypes.add(floattype);
        TypeStruct complexStruct =
            new TypeStruct(null, "Complex", fields, ftypes);
        structs.add(complexStruct);
        
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
        throws java.io.IOException,
               antlr.RecognitionException, 
               antlr.TokenStreamException
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
    public void lowerIRToJava(boolean libraryFormat)
    {
        /* What's the right order for these?  Clearly generic
         * things like MakeBodiesBlocks need to happen first.
         * I don't think there's actually a problem running
         * MoveStreamParameters after DoComplexProp, since
         * this introduces only straight assignments which the
         * Java front-end can handle.  OTOH,
         * MoveStreamParameters introduces references to
         * "this", which doesn't exist. */
        prog = (Program)prog.accept(new MakeBodiesBlocks());
        prog = (Program)prog.accept(new ExtractRightShifts(varGen));
        prog = (Program)prog.accept(new ExtractVectorsInCasts(varGen));
        prog = (Program)prog.accept(new SeparateInitializers());
        prog = (Program)prog.accept(new DisambiguateUnaries(varGen));
        prog = (Program)prog.accept(new NoRefTypes());
        prog = (Program)prog.accept(new FindFreeVariables());
        if (!libraryFormat)
            prog = (Program)prog.accept(new NoticePhasedFilters());
        prog = (Program)prog.accept(new DoComplexProp(varGen));
        prog = (Program)prog.accept(new EliminateArrayRange(varGen));
        beforeUnvectorizing = prog;
        prog = (Program)prog.accept(new GenerateCopies(varGen));
        prog = (Program)prog.accept(new ComplexToStruct());
        prog = (Program)prog.accept(new SeparateInitializers());
        prog = (Program)prog.accept(new EnqueueToFunction());
        prog = (Program)prog.accept(new InsertIODecls(libraryFormat));
//        prog = (Program)prog.accept(new InsertInitConstructors(varGen));
        prog = (Program)prog.accept(new MoveStreamParameters());
        prog = (Program)prog.accept(new NameAnonymousFunctions());        
        prog = (Program)prog.accept(new TrimDumbDeadCode());        
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
    
    public Program preprocessProgram(Program prog) {
        //invoke post-parse passes
        prog = (Program)prog.accept(new FunctionParamExtension());
        prog = (Program)prog.accept(new ConstantReplacer(params.defines));
        return prog;
    }
    
    public void partialEvalAndSolve(){
    	lowerIRToJava(!params.libraryFormat);
        
        assert oracle != null;
        try
        {
            OutputStream outStream;
        	if(params.fakeSolver)
        		outStream = new NullStream();
        	else if(params.outputFile != null)
                outStream = new FileOutputStream(params.outputFile);
            else
                outStream = System.out;
            ProduceBooleanFunctions partialEval =
                new ProduceBooleanFunctions (null, varGen, oracle,
                                             new PrintStream(outStream),
                                             params.unrollAmt, params.inlineAmt);
            System.out.println("MAX LOOP UNROLLING = " + params.unrollAmt);
            System.out.println("MAX FUNC INLINING  = " + params.inlineAmt);
            prog.accept( partialEval );
            outStream.flush();
        }
        catch (java.io.IOException e)
        {
            //e.printStackTrace(System.err);
            throw new RuntimeException(e);
        }

        
        boolean worked = params.fakeSolver || solve(oracle);
        if(!worked){
        	throw new RuntimeException("The sketch could not be resolved.");
        }
        
        try{
        	String fname = params.outputFile + ".tmp";
        	File f = new File(fname);
            FileInputStream fis = new FileInputStream(f); 
            BufferedInputStream bis = new BufferedInputStream(fis);  
            LineNumberReader lir = new LineNumberReader(new InputStreamReader(bis));
        	oracle.loadFromStream(lir);        	
        }
        catch (java.io.IOException e)
        {
            //e.printStackTrace(System.err);
            throw new RuntimeException(e);
        }
       

    }
    
    public void eliminateStar(){
    	 finalCode =
             (Program) beforeUnvectorizing.accept (
                 new EliminateStar(oracle, params.unrollAmt, params.inlineAmt));
         finalCode =
             (Program) finalCode.accept (
                 new EliminateStar(oracle, params.unrollAmt, params.inlineAmt, 3));
    }
    
    protected String getOutputFileName() {
        String resultFile = params.resultFile;
     	if(resultFile==null) {
     		resultFile=params.inputFiles.get(0);
     	}
 		if(resultFile.lastIndexOf("/")>=0)
 			resultFile=resultFile.substring(resultFile.lastIndexOf("/")+1);
 		if(resultFile.lastIndexOf("\\")>=0)
 			resultFile=resultFile.substring(resultFile.lastIndexOf("\\")+1);
 		if(resultFile.lastIndexOf(".")>=0)
 			resultFile=resultFile.substring(0,resultFile.lastIndexOf("."));
 		return resultFile;
    }
    
    protected void outputCCode() {
        String resultFile = getOutputFileName();
        
        String hcode = (String)finalCode.accept(new NodesToH(resultFile));
        String ccode = (String)finalCode.accept(new NodesToC(varGen,resultFile));
        if(!params.outputCFiles){
        	System.out.println(hcode);
        	System.out.println(ccode);
        }else{
        	try{
        		{
	        		Writer outWriter = new FileWriter(params.outputCDir+resultFile+".h");
	            	outWriter.write(hcode);
	                outWriter.flush();
	                outWriter.close();
	        		outWriter = new FileWriter(params.outputCDir+resultFile+".cpp");
	            	outWriter.write(ccode);
	                outWriter.flush();
	                outWriter.close();
        		}
                if(params.outputTest) {
                	String testcode=(String)beforeUnvectorizing.accept(new NodesToCTest(resultFile));
                	Writer outWriter = new FileWriter(params.outputCDir+resultFile+"_test.c");
            		outWriter.write(testcode);
                    outWriter.flush();
                    outWriter.close();
                }
                if(params.outputScript) {
            		Writer outWriter = new FileWriter(params.outputCDir+"script");
            		outWriter.write("#!/bin/sh\n");
            		if(params.outputTest)
            			outWriter.write("g++ -o "+resultFile+" "+resultFile+".cpp "+resultFile+"_test.c\n");
            		else
            			outWriter.write("g++ -c "+resultFile+".c\n");
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
    
    public void generateCode(){
    	 if(params.doVectorization) {
         	finalCode = (Program)finalCode.accept(new AssembleInitializers());
         	finalCode=(Program) finalCode.accept(new BitVectorPreprocessor(varGen));
         	finalCode=(Program) finalCode.accept(new BitTypeRemover(varGen));
         	finalCode=(Program) finalCode.accept(new SimplifyExpressions());
         }
         outputCCode();
    }
    
    
    public void run()
    {        
        if (params.printHelp)
        {
            printUsage();
            return;
        }
        
        parseProgram();
        prog.accept(new SimpleCodePrinter());
        // RenameBitVars is buggy!! prog = (Program)prog.accept(new RenameBitVars());
        if (!SemanticChecker.check(prog))
            throw new IllegalStateException("Semantic check failed");
        prog = (Program)prog.accept(new AssignLoopTypes());
        if (prog == null)
            throw new IllegalStateException();

        oracle = new ValueOracle( new SequentialHoleTracker(varGen) );
        partialEvalAndSolve();
        eliminateStar();
        generateCode();
        
    }
    
    private boolean solve(ValueOracle oracle){
        final List<Boolean> done=new ArrayList<Boolean>();
        
        Thread stopper=null;
        if(params.hasTimeout){
	        stopper = new Thread() {
				@Override
				public void run()
				{
					try {
						sleep(params.timeout*60*1000);
					}
					catch (InterruptedException e) {
						e.printStackTrace();
					}
					if(!done.isEmpty()) return;
					System.out.println("Time limit exceeded!");
					System.exit(1);
				}
	        	
	        };
	        stopper.start();
        }
        System.out.println("OFILE = " + params.outputFile);
        String command = (params.sbitPath!=null? params.sbitPath : "") + "SBitII";
        if(params.incremental){
        	boolean isSolved = false;
        	int bits=0;
        	for(bits=1; bits<=params.incermentalAmt; ++bits){
        		System.out.println("TRYING SIZE " + bits);
        		String[] commandLine = new String[ 5 + params.commandLineOptions.size()];
        		commandLine[0] = command;
        		commandLine[1] = "-overrideCtrls"; 
        		commandLine[2] = "" + bits;
        		for(int i=0; i< params.commandLineOptions.size(); ++i){
        			commandLine[3+i] = params.commandLineOptions.elementAt(i);
        		}
        		commandLine[commandLine.length -2 ] = params.outputFile;
        		commandLine[commandLine.length -1 ] = params.outputFile + ".tmp";        		
    	        boolean ret = runSolver(commandLine, bits);
    	        if(ret){
    	        	isSolved = true;
    	        	break;
    	        }else{
    	        	System.out.println("Size " + bits + " is not enough");
    	        }
        	}
        	if(!isSolved){
	        	System.out.println("The sketch can not be resolved");
	        	return false;
	        }
	        System.out.println("Succeded with " + bits + " bits for integers");	        
        	oracle.capStarSizes(bits);
        }else{
        	String[] commandLine = new String[ 3 + params.commandLineOptions.size()];
    		commandLine[0] = command;    		
    		for(int i=0; i< params.commandLineOptions.size(); ++i){
    			commandLine[1+i] = params.commandLineOptions.elementAt(i);
    		}
    		commandLine[commandLine.length -2 ] = params.outputFile;
    		commandLine[commandLine.length -1 ] = params.outputFile + ".tmp";	        
	        boolean ret = runSolver(commandLine, 0);
	        if(!ret){
	        	System.out.println("The sketch can not be resolved");
	        	return false;
	        }
        }
        done.add(Boolean.TRUE);
        if(params.hasTimeout){
        	stopper.interrupt();
        }
        return true;
    }
    
    
    private boolean runSolver(String[] commandLine, int i){
    	for(int k=0;k<commandLine.length;k++) 
    		System.out.print(commandLine[k]+" "); 
    	System.out.println("");
    	
        Runtime rt = Runtime.getRuntime();        
        try
        {
	        Process proc = rt.exec(commandLine);   
	        InputStream output = proc.getInputStream();
	        InputStream stdErr = proc.getErrorStream();
	        InputStreamReader isr = new InputStreamReader(output);
	        InputStreamReader errStr = new InputStreamReader(stdErr);
	        BufferedReader br = new BufferedReader(isr);
	        BufferedReader errBr = new BufferedReader(errStr);
	        String line = null;
	        while ( (line = br.readLine()) != null)
	            System.out.println(i + "  " + line);
	        while ( (line = errBr.readLine()) != null)
	            System.err.println(line);
	        int exitVal = proc.waitFor();
	        System.out.println("Process exitValue: " + exitVal);
	        if(exitVal != 0) {
	        	return false;
	        }
        }
        catch (java.io.IOException e)
        {
            //e.printStackTrace(System.err);
            throw new RuntimeException(e);
        }
        catch (InterruptedException e)
        {
            //e.printStackTrace(System.err);
            throw new RuntimeException(e);
        }
        return true;
    }
    
    public static void main(String[] args)
    {
    	
        new ToSBit(new CommandLineParams(args)).run();
        System.exit(0);
    }
}

