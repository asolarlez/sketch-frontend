package sketch.compiler.solvers;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;

import sketch.compiler.CommandLineParamManager;
import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.dataflow.recursionCtrl.RecursionControl;
import sketch.compiler.solvers.constructs.StaticHoleTracker;
import sketch.compiler.solvers.constructs.ValueOracle;
import sketch.util.Misc;
import sketch.util.NullStream;
import sketch.util.ProcessStatus;
import sketch.util.SynchronousTimedProcess;

public class SATBackend {

	final CommandLineParamManager params;
	String solverErrorStr;
	final RecursionControl rcontrol;
	final TempVarGen varGen;
	protected ValueOracle oracle;
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


	protected void partialEval(Program prog, OutputStream outStream){
		sketch.compiler.dataflow.nodesToSB.ProduceBooleanFunctions
		partialEval =
			new sketch.compiler.dataflow.nodesToSB.ProduceBooleanFunctions (varGen, oracle,
				new PrintStream(outStream)
			//	System.out
			,
			params.flagValue("unrollamnt"), rcontrol, tracing);
			log ("MAX LOOP UNROLLING = " + params.flagValue("unrollamnt"));
			log ("MAX FUNC INLINING  = " + params.flagValue("inlineamnt"));
			prog.accept( partialEval );
			
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


			partialEval(prog, outStream);
	
			
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

		String fname = params.sValue("output")+ ".tmp";
		extractOracleFromOutput(fname);
		return worked;
	}

	
	protected void extractOracleFromOutput(String fname){
		try{		
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
	}

	public void addToBackendParams(List<String> params){
		commandLineOptions.addAll(params);
	}



	

	private boolean solve(ValueOracle oracle){

		log ("OFILE = " + params.sValue("output"));
		
		if(params.hasFlag("incremental")){
			boolean isSolved = false;
			int bits=0;
			int maxBits = params.flagValue("incremental");
			for(bits=1; bits<=maxBits; ++bits){
				log ("TRYING SIZE " + bits);			
				String[] extra = {"-overrideCtrls", "" + bits};
				String[] commandLine = params.getBackendCommandline(commandLineOptions, extra);
				
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
			String[] extra = {};
			String[] commandLine = params.getBackendCommandline(commandLineOptions, extra);
			boolean ret = runSolver(commandLine, 0);
			if(!ret){
				log (0, "The sketch cannot be resolved");
				System.err.println(solverErrorStr);
				return false;
			}
		}
		return true;
	}


	protected void logCmdLine(String[] commandLine){
		String cmdLine = "";
		for (String a : commandLine)  cmdLine += a + " ";
		log ("Launching: "+ cmdLine);
	}
	
	private boolean runSolver(String[] commandLine, int i) {
		logCmdLine(commandLine);
		
		ProcessStatus status = null;
		try {
			status = (new SynchronousTimedProcess (params.flagValue("timeout"),
												   commandLine)).run (false);
			
			
			
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
