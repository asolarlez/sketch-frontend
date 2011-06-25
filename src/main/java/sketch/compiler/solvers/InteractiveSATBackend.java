package sketch.compiler.solvers;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Vector;

import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.dataflow.recursionCtrl.RecursionControl;
import sketch.compiler.main.cmdline.SketchOptions;
import sketch.compiler.solvers.constructs.StaticHoleTracker;
import sketch.compiler.solvers.constructs.ValueOracle;
import sketch.util.InteractiveTimedProcess;

public class InteractiveSATBackend extends SATBackend {

	public InteractiveSATBackend(SketchOptions options,
			RecursionControl rcontrol, TempVarGen varGen) {
		super(options, rcontrol, varGen);
		ht = new StaticHoleTracker(varGen);
		// TODO Auto-generated constructor stub
	}

	InteractiveTimedProcess proc;
	final StaticHoleTracker ht;
	
	public void initializeSolver(){
		String interactiveFlag = "-interactive";
		Vector<String> opts = options.getBackendOptions();
		if (!opts.contains(interactiveFlag)) {
		    opts.add(interactiveFlag);
		}
		String[] commandLine = getBackendCommandline(options.getBackendOptions());
		{
			String cmdLine = "";
			for (String a : commandLine)  cmdLine += a + " ";
			log (0, "Launching: "+ cmdLine);
		}
		try{
			proc = new InteractiveTimedProcess(options.solverOpts.timeout, commandLine);
			proc.run();		
		}catch (java.io.IOException e)	{
			if (null != proc.status() && proc.status().killedByTimeout) {
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
			
			if(options.feOpts.keepTmp){
				/*If the keeptmpfiles flag is set, we put a copy of whatever we write to the 
				 * output stream into the tmp file.*/
				final OutputStream outStream = new FileOutputStream(options.feOpts.output + ver);
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
		String fname = options.feOpts.output + ".tmp";
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
