package streamit.frontend.solvers;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;

import streamit.frontend.CommandLineParamManager;
import streamit.frontend.nodes.Program;
import streamit.frontend.nodes.TempVarGen;
import streamit.frontend.stencilSK.StaticHoleTracker;
import streamit.frontend.tosbit.ValueOracle;
import streamit.frontend.tosbit.recursionCtrl.RecursionControl;import streamit.misc.ProcessKillerThread;

public class SATBackend {

	
	
	private static class NullStream extends OutputStream {
		public void flush() throws IOException {}
		public void close() throws IOException {}
		public void write(int arg0) throws IOException {}
	}
	
	final CommandLineParamManager params;
	String solverErrorStr;
	final RecursionControl rcontrol;
	final TempVarGen varGen;
	private ValueOracle oracle;
	private boolean tracing = false;
	public final List<String> commandLineOptions;
	
	
	public SATBackend(CommandLineParamManager params, RecursionControl rcontrol, TempVarGen varGen){
		this.params = params;
		this.rcontrol =rcontrol;
		this.varGen = varGen;
		commandLineOptions = params.backendOptions;
	}
	
	public void activateTracing(){
		tracing = true;
	}
	
	
	public boolean partialEvalAndSolve(Program prog){
		oracle = new ValueOracle( new StaticHoleTracker(varGen) );
		System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
		//prog.accept(new SimpleCodePrinter());
		assert oracle != null;
		try
		{
			OutputStream outStream;
			if(params.hasFlag("fakesolver"))
				outStream = new NullStream();
			else if(params.sValue("output") != null)
				outStream = new FileOutputStream(params.sValue("output"));
			else
				outStream = System.out;


			streamit.frontend.experimental.nodesToSB.ProduceBooleanFunctions
			partialEval =
				new streamit.frontend.experimental.nodesToSB.ProduceBooleanFunctions (varGen, oracle,
					new PrintStream(outStream)
				//	System.out
				,
				params.flagValue("unrollamnt"), rcontrol, tracing);
			/*
             ProduceBooleanFunctions partialEval =
                new ProduceBooleanFunctions (null, varGen, oracle,
                                             new PrintStream(outStream),
                                             params.unrollAmt, newRControl()); */
			System.out.println("MAX LOOP UNROLLING = " + params.flagValue("unrollamnt"));
			System.out.println("MAX FUNC INLINING  = " + params.flagValue("inlineamnt"));
			prog.accept( partialEval );
			outStream.flush();
			outStream.close();
		}
		catch (java.io.IOException e)
		{
			//e.printStackTrace(System.err);
			throw new RuntimeException(e);
		}


		boolean worked = params.hasFlag("fakesolver") || solve(oracle);

		{
			java.io.File fd = new File(params.sValue("output"));
			if(fd.exists() && !params.hasFlag("keeptmpfiles")){
				boolean t = fd.delete();
				if(!t){
					System.out.println("couldn't delete file" + fd.getAbsolutePath());
				}
			}else{
				System.out.println("Not Deleting");
			}
		}

		if(!worked && !params.hasFlag("forcecodegen")){
			throw new RuntimeException("The sketch could not be resolved.");
		}

		try{
			String fname = params.sValue("output")+ ".tmp";
			File f = new File(fname);
			FileInputStream fis = new FileInputStream(f);
			BufferedInputStream bis = new BufferedInputStream(fis);
			LineNumberReader lir = new LineNumberReader(new InputStreamReader(bis));
			oracle.loadFromStream(lir);
			fis.close();
			java.io.File fd = new File(fname);
			if(fd.exists() && !params.hasFlag("keeptmpfiles")){
				fd.delete();
			}
		}
		catch (java.io.IOException e)
		{
			//e.printStackTrace(System.err);
			throw new RuntimeException(e);
		}

		return worked;
	}

	
	public void addToBackendParams(List<String> params){		
		commandLineOptions.addAll(params);
	}
	
	
	
	
	private boolean solve(ValueOracle oracle){		

		

		System.out.println("OFILE = " + params.sValue("output"));
		String command = (params.hasFlag("sbitpath") ? params.sValue("sbitpath") : "") + "SBitII";
		if(params.hasFlag("incremental")){
			boolean isSolved = false;
			int bits=0;
			int maxBits = params.flagValue("incremental");
			for(bits=1; bits<=maxBits; ++bits){
				System.out.println("TRYING SIZE " + bits);
				String[] commandLine = new String[ 5 + commandLineOptions.size()];
				commandLine[0] = command;
				commandLine[1] = "-overrideCtrls";
				commandLine[2] = "" + bits;
				for(int i=0; i< commandLineOptions.size(); ++i){
					commandLine[3+i] = commandLineOptions.get(i);
				}
				commandLine[commandLine.length -2 ] = params.sValue("output") ;
				commandLine[commandLine.length -1 ] = params.sValue("output") + ".tmp";
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
				System.err.println(solverErrorStr);
				return false;
			}
			System.out.println("Succeded with " + bits + " bits for integers");
			oracle.capStarSizes(bits);
		}else{
			String[] commandLine = new String[ 3 + commandLineOptions.size()];
			commandLine[0] = command;
			for(int i=0; i< commandLineOptions.size(); ++i){
				commandLine[1+i] = commandLineOptions.get(i);
			}
			commandLine[commandLine.length -2 ] = params.sValue("output");
			commandLine[commandLine.length -1 ] = params.sValue("output") + ".tmp";
			boolean ret = runSolver(commandLine, 0);
			if(!ret){
				System.out.println("The sketch can not be resolved");
				System.err.println(solverErrorStr);
				return false;
			}
		}
		return true;
	}
	

	private boolean runSolver(String[] commandLine, int i){
		for(int k=0;k<commandLine.length;k++)
			System.out.print(commandLine[k]+" ");
		System.out.println("");

		Runtime rt = Runtime.getRuntime();
		ProcessKillerThread stopper=null;
		try
		{
			Process proc = rt.exec(commandLine);
			if(params.hasFlag("timeout")) {
				System.out.println("Timing out after " + params.flagValue("timeout") + " minutes.");
				stopper = new ProcessKillerThread (proc, params.flagValue ("timeout"));
				stopper.start();
			}

			InputStream output = proc.getInputStream();
			InputStream stdErr = proc.getErrorStream();
			InputStreamReader isr = new InputStreamReader(output);
			InputStreamReader errStr = new InputStreamReader(stdErr);
			BufferedReader br = new BufferedReader(isr);
			BufferedReader errBr = new BufferedReader(errStr);
			String line = null;
			while ( (line = br.readLine()) != null){
				if(line.length() > 2){
					if(!(line.charAt(0) == '-' && line.contains("->"))){
						System.out.println(i + "  " + line);
					}
				}
			}
			solverErrorStr = "";
			while ( (line = errBr.readLine()) != null)
				solverErrorStr += line + "\n";
			int exitVal = proc.waitFor();
			System.out.println("Process exitValue: " + exitVal);
			if(exitVal != 0) {
				return false;
			}
		}
		catch (java.io.IOException e)
		{
			if (stopper != null && stopper.didKill ()) {
				System.err.println ("Warning: lost some output from backend because of timeout.");
				return false;
			} else {
				//e.printStackTrace(System.err);
				throw new RuntimeException(e);
			}
		}
		catch (InterruptedException e)
		{
			//e.printStackTrace(System.err);
			throw new RuntimeException(e);
		}
		finally {
			if (stopper != null)
				stopper.abort ();
		}
		return true;
	}





	/**
	 * @param oracle the oracle to set
	 */
	public void setOracle(ValueOracle oracle) {
		this.oracle = oracle;
	}





	/**
	 * @return the oracle
	 */
	public ValueOracle getOracle() {
		return oracle;
	}

}
