package streamit.frontend.solvers;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import streamit.frontend.CommandLineParamManager;
import streamit.frontend.nodes.Program;
import streamit.frontend.nodes.TempVarGen;
import streamit.frontend.stencilSK.StaticHoleTracker;
import streamit.frontend.tosbit.ValueOracle;
import streamit.frontend.tosbit.recursionCtrl.RecursionControl;
import streamit.misc.InteractiveTimedProcess;
import streamit.misc.Misc;

public class InteractiveSATBackend extends SATBackend {

	public InteractiveSATBackend(CommandLineParamManager params,
			RecursionControl rcontrol, TempVarGen varGen) {
		super(params, rcontrol, varGen);
		ht = new StaticHoleTracker(varGen);
		// TODO Auto-generated constructor stub
	}

	InteractiveTimedProcess proc;
	final StaticHoleTracker ht;
	
	public void initializeSolver(){
		String[] extra = {"-interactive"};
		String[] commandLine = params.getBackendCommandline(commandLineOptions, extra);
		{
			String cmdLine = "";
			for (String a : commandLine)  cmdLine += a + " ";
			log (0, "Launching: "+ cmdLine);
		}
		try{
			proc = new InteractiveTimedProcess(params.flagValue("timeout"),commandLine);
			proc.run();		
		}catch (java.io.IOException e)	{
			if (null != proc.status() && proc.status().killed) {
				System.err.println ("Warning: lost some output from backend because of timeout.");
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
	
	int ver = 0;
	
	public boolean partialEvalAndSolve(Program prog){
		oracle = new ValueOracle( ht );
		log ("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
			
		try{						
			final OutputStream pos = proc.getOutputStream(); 
			OutputStream multiplex = null;
			
			if(params.hasFlag("keeptmpfiles")){
				/*If the keeptmpfiles flag is set, we put a copy of whatever we write to the 
				 * output stream into the tmp file.*/
				final OutputStream outStream = new FileOutputStream(params.sValue("output") + ver);
				multiplex = new OutputStream(){
					@Override
					public void write(int b) throws IOException {
						pos.write(b);
						outStream.write(b);
					}			
					@Override
					public void flush() throws IOException{
						pos.flush();
						outStream.flush();
					}
				};
			}else{
				multiplex = pos;
			}
			++ver;
			partialEval(prog, multiplex);						
			multiplex.flush();			
		}catch(IOException ioe){
			throw new RuntimeException(ioe.getMessage());
		}
		proc.waitForAnswer("COMPLETED", false);
		String fname = params.sValue("output")+ ".tmp";
		extractOracleFromOutput(fname);
		return proc.stillActive();
	}
	
	public void cleanup(){
		PrintStream ps = new PrintStream(proc.getOutputStream());
		ps.println("exit();");
		ps.flush();		
		proc.cleanup();
	}
	
	public SolutionStatistics getLastSolutionStats () {
		
		SATSolutionStatistics lastSolveStats = parseStats (proc.status().out);
		
		lastSolveStats.success = (0 == proc.status().exitCode);
		log (2, "Stats for last run:\n"+ lastSolveStats);

		solverErrorStr = proc.status().err;
		log ("Solver exit value: "+ proc.status().exitCode);
		
		return lastSolveStats;
	}
	
	
	
}
