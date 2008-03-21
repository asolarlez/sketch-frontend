package streamit.frontend.solvers;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import streamit.frontend.CommandLineParamManager;
import streamit.frontend.nodes.Program;
import streamit.frontend.nodes.TempVarGen;
import streamit.frontend.stencilSK.StaticHoleTracker;
import streamit.frontend.tosbit.ValueOracle;
import streamit.frontend.tosbit.recursionCtrl.RecursionControl;import streamit.misc.Misc;
import streamit.misc.NullStream;
import streamit.misc.ProcessStatus;
import streamit.misc.SynchronousTimedProcess;

public class SATBackend {

	final CommandLineParamManager params;
	String solverErrorStr;
	final RecursionControl rcontrol;
	final TempVarGen varGen;
	private ValueOracle oracle;
	private boolean tracing = false;
	public final List<String> commandLineOptions;
	private SATSolutionStatistics lastSolveStats;

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
		log ("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
		//prog.accept(new SimpleCodePrinter());
		assert oracle != null;
		try
		{
			OutputStream outStream;
			if(params.hasFlag("fakesolver"))
				outStream = NullStream.INSTANCE;
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
			log ("MAX LOOP UNROLLING = " + params.flagValue("unrollamnt"));
			log ("MAX FUNC INLINING  = " + params.flagValue("inlineamnt"));
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
					log (0, "couldn't delete file" + fd.getAbsolutePath());
				}
			}else{
				log ("Not Deleting");
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



		log ("OFILE = " + params.sValue("output"));
		String command = (params.hasFlag("sbitpath") ? params.sValue("sbitpath") : "") + "SBitII";
		if(params.hasFlag("incremental")){
			boolean isSolved = false;
			int bits=0;
			int maxBits = params.flagValue("incremental");
			for(bits=1; bits<=maxBits; ++bits){
				log ("TRYING SIZE " + bits);
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
					log ("Size " + bits + " is not enough");
				}
			}
			if(!isSolved){
				log (0, "The sketch cannot be resolved");
				System.err.println(solverErrorStr);
				return false;
			}
			log ("Succeded with " + bits + " bits for integers");
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
				log (0, "The sketch cannot be resolved");
				System.err.println(solverErrorStr);
				return false;
			}
		}
		return true;
	}


	private boolean runSolver(String[] commandLine, int i) {
		String cmdLine = "";
		for (String a : commandLine)  cmdLine += a + " ";
		log ("Launching: "+ cmdLine);

		ProcessStatus status = null;
		try {
			status = (new SynchronousTimedProcess (params.flagValue("timeout"),
												   commandLine)).run ();
			if (verbose ()) {
				Matcher m = Pattern.compile ("^[^\\->]+.*$",
											 Pattern.MULTILINE).matcher (status.out);
				while (m.find ())  log (m.group ());
			}

			lastSolveStats = parseStats (status.out);
			lastSolveStats.success = (0 == status.exitCode);
			log (2, "Stats for last run:\n"+ lastSolveStats);

			solverErrorStr = status.err;
			log ("Solver exit value: "+ status.exitCode);

			return lastSolveStats.success;
		}
		catch (java.io.IOException e)	{
			if (null != status && status.killed) {
				System.err.println ("Warning: lost some output from backend because of timeout.");
				return false;
			} else {
				//e.printStackTrace(System.err);
				throw new RuntimeException(e);
			}
		}
		catch (InterruptedException e) {
			//e.printStackTrace(System.err);
			throw new RuntimeException(e);
		}
	}

	protected SATSolutionStatistics parseStats (String out) {
		SATSolutionStatistics s = new SATSolutionStatistics ();
		List<String> res;
		String NL = "(?:\\r\\n|\\n|\\r)";

		// XXX: using max virtual mem; maybe resident or private is better
		res = Misc.search (out,
				"Total elapsed time \\(ms\\):\\s+(\\d+(?:\\.\\d+)?)"+ NL +
				"Model building time \\(ms\\):\\s+(\\d+(?:\\.\\d+)?)"+ NL +
				"Solution time \\(ms\\):\\s+(\\d+(?:\\.\\d+)?)"+ NL +
				"Max virtual mem \\(bytes\\):\\s+(\\d+)");		
		if(res != null){
			s.elapsedTimeMs = (long) (Float.parseFloat (res.get (0)));
			s.modelBuildingTimeMs = (long) (Float.parseFloat (res.get (1)));
			s.solutionTimeMs = (long) (Float.parseFloat (res.get (2)));
			s.maxMemUsageBytes = Long.parseLong (res.get (3));
		}else{
			s.elapsedTimeMs = -1; 
			s.modelBuildingTimeMs = -1; 
			s.solutionTimeMs = -1; 
			s.maxMemUsageBytes = -1; 
		}

		res = Misc.search (out, "SKETCH nodes = (\\d+)");
		if(res != null){
			s.numNodesInitial = Long.parseLong (res.get (0));
		}else{
			s.numNodesInitial = -1;
		}

		res = Misc.search (out, "Final Problem size: Problem nodes = (\\d+)");
		if( null != res ){
			s.numNodesFinal = Long.parseLong (res.get (0));
		}else{
			s.numNodesFinal = -1;
		}

		res = Misc.search (out, "# OF CONTROLS:\\s+(\\d+)");
		if(null != res){
			s.numControls = Long.parseLong (res.get (0));
		}else{
			s.numControls = -1;
		}

		res = Misc.search (out, "ctrlSize = (\\d+)");
		if(null != res){
			s.numControlBits = Long.parseLong (res.get (0));
		}else{
			s.numControlBits = -1;
		}

		return s;
	}

	public SolutionStatistics getLastSolutionStats () {
		return lastSolveStats;
	}

	protected boolean verbose () {
		return params.flagValue ("verbosity") >= 3;
	}

	// TODO: duplication is absurd now, need to use the Logger class
	protected void log (String msg) {  log (3, msg);  }
	protected void log (int level, String msg) {
		if (params.flagValue ("verbosity") >= level)
			System.out.println ("[SATBackend] "+ msg);
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
