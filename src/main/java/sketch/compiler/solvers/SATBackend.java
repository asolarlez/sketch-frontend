package sketch.compiler.solvers;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Vector;

import org.apache.commons.io.FileUtils;

import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.dataflow.recursionCtrl.RecursionControl;
import sketch.compiler.main.PlatformLocalization;
import sketch.compiler.main.seq.SequentialSketchOptions;
import sketch.compiler.passes.optimization.CostFcnAssert;
import sketch.compiler.passes.structure.HasMinimize;
import sketch.compiler.solvers.constructs.StaticHoleTracker;
import sketch.compiler.solvers.constructs.ValueOracle;
import sketch.util.DebugOut;
import sketch.util.Misc;
import sketch.util.NullStream;
import sketch.util.ProcessStatus;
import sketch.util.SynchronousTimedProcess;
import sketch.util.datastructures.IntRange;

public class SATBackend {

	String solverErrorStr;
	final RecursionControl rcontrol;
	final TempVarGen varGen;
	protected ValueOracle oracle;
	private boolean tracing = false;
	private SATSolutionStatistics lastSolveStats;
    public final SequentialSketchOptions options;

	public SATBackend(SequentialSketchOptions options, 
	        RecursionControl rcontrol, TempVarGen varGen)
	{
		this.options = options;
		this.rcontrol =rcontrol;
		this.varGen = varGen;
	}

	public void activateTracing(){
		tracing = true;
	}
	
	public String[] getBackendCommandline(Vector<String> commandLineOptions){
	    PlatformLocalization pl = PlatformLocalization.getLocalization();
        String cegisBinary = pl.getCegisPath();
        commandLineOptions.insertElementAt(cegisBinary, 0);
        commandLineOptions.add(options.getTmpSketchFilename());
        commandLineOptions.add(options.getTmpSketchFilename() + ".tmp");
        return commandLineOptions.toArray(new String[0]);
    }

    protected void partialEval(Program prog, OutputStream outStream) {
        sketch.compiler.dataflow.nodesToSB.ProduceBooleanFunctions partialEval =
                new sketch.compiler.dataflow.nodesToSB.ProduceBooleanFunctions(varGen,
                        oracle, new PrintStream(outStream)
                        // System.out
                        , options.bndOpts.unrollAmnt, rcontrol, tracing);
        log("MAX LOOP UNROLLING = " + options.bndOpts.unrollAmnt);
        log("MAX FUNC INLINING  = " + options.bndOpts.inlineAmnt);
        prog.accept(partialEval);
    }

    public boolean partialEvalAndSolve(Program prog) {
        oracle = new ValueOracle(new StaticHoleTracker(varGen));
        log("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
        // prog.accept(new SimpleCodePrinter());
        assert oracle != null;
        writeProgramToBackendFormat(prog);

        final HasMinimize hasMinimize = new HasMinimize();
        hasMinimize.visitProgram(prog);

        final String tmpSketchFilename = options.getTmpSketchFilename();
        File sketchOutputFile = new File(tmpSketchFilename + ".tmp");
        File bestValueFile =
                hasMinimize.hasMinimize() ? new File(tmpSketchFilename + ".best")
                        : sketchOutputFile;

        boolean worked = false;
        if (options.debugOpts.fakeSolver) {
            worked = true;
        } else if (hasMinimize.hasMinimize()) {
            IntRange currRange = IntRange.inclusive(0, 2 * options.bndOpts.costEstimate);
            float timeout =
                    Math.max(1.f, options.solverOpts.timeout) / ((float) (1 << 10));
            for (int a = 0; a < 100 && !currRange.isEmpty(); a++) {
                // choose a value from not-yet-inspected values
                final int currValue = (int) currRange.middle();
                System.err.println("current: " + currValue + " \\in " + currRange);
                log(2, "current range: " + currValue + " \\in " + currRange);

                // rewrite the minimize statements in the program
                final CostFcnAssert costFcnAssert = new CostFcnAssert(currValue);
                writeProgramToBackendFormat((Program) costFcnAssert.visitProgram(prog));

                // actually run the solver
                boolean currResult = solve(oracle, timeout);
                worked |= currResult;
                if (!currResult) {
                    // didn't work. explore the upper interval, and explore more if we
                    // haven't
                    // found any solution yet
                    currRange = currRange.nextInfemum(currValue);
                    if (!worked) {
                        timeout *= 2;
                        currRange = currRange.nextMax(currRange.max * 2);
                        // if the sketch is buggy, don't take too much time to fail.
                        if (a >= 10) {
                            break;
                        }
                    }
                } else {
                    // try the next range, and save the result
                    currRange = currRange.nextSupremum(currValue);
                    try {
                        FileUtils.copyFile(sketchOutputFile, bestValueFile);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } else {
            worked = solve(oracle, options.solverOpts.timeout);
        }

        {
            java.io.File fd = new File(options.getTmpSketchFilename());
            if (fd.exists() && !options.feOpts.keepTmp) {
                boolean t = fd.delete();
                if (!t) {
                    log(0, "couldn't delete file" + fd.getAbsolutePath());
                }
            } else {
                log("Not Deleting");
            }
        }

        if (!worked && !options.feOpts.forceCodegen) {
            if (SynchronousTimedProcess.wasKilled.get()) {
                System.exit(1);
            }
            throw new RuntimeException("The sketch could not be resolved.");
        }

        extractOracleFromOutput(bestValueFile.getPath());
        return worked;
    }

    public void writeProgramToBackendFormat(Program prog) {
        try {
            OutputStream outStream = null;
            if (options.debugOpts.fakeSolver)
                outStream = NullStream.INSTANCE;
            else
                // if (options.getTmpName != null)
                outStream = new FileOutputStream(options.getTmpSketchFilename());
            // else
            // DebugOut.assertFalse("no temporary filename defined.");
            // outStream = System.out;

            // visit the program and write out the program in the backend's input format.
            partialEval(prog, outStream);

            outStream.flush();
            outStream.close();
        } catch (java.io.IOException e) {
            // e.printStackTrace(System.err);
            throw new RuntimeException(e);
        }
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
			if(fd.exists() && !options.feOpts.keepTmp){
				fd.delete();
			}
		}
		catch (java.io.IOException e)
		{
			//e.printStackTrace(System.err);
			throw new RuntimeException(e);
		}		
	}



	

	private boolean solve(ValueOracle oracle, float timeoutMins){

		log ("OFILE = " + options.feOpts.output);
		
		if (options.bndOpts.incremental.isSet) {
			boolean isSolved = false;
			int bits=0;
			int maxBits = options.bndOpts.incremental.value;
			for(bits=1; bits<=maxBits; ++bits){
				log ("TRYING SIZE " + bits);			
				Vector<String> backendOptions = options.getBackendOptions();
				backendOptions.add("-overrideCtrls");
				backendOptions.add("" + bits);
				String[] commandLine = getBackendCommandline(backendOptions);
				
				boolean ret = runSolver(commandLine, bits, timeoutMins);
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
        } else {
            Vector<String> backendOptions = options.getBackendOptions();
            String[] commandLine = getBackendCommandline(backendOptions);
			boolean ret = runSolver(commandLine, 0, timeoutMins);
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

    private boolean runSolver(String[] commandLine, int i, float timeoutMins) {
        logCmdLine(commandLine);

        SynchronousTimedProcess proc;
        try {
            proc = new SynchronousTimedProcess(timeoutMins, commandLine);
        } catch (IOException e) {
            DebugOut.printFailure("Could not instantiate solver (CEGIS) process.");
            throw new RuntimeException(e);
        }

        final ProcessStatus status = proc.run(false);

        // deal with killed states
        if (!status.killedByTimeout) {
            if (status.exitCode != 0 && status.err.contains("I've been killed.")) {
                DebugOut.printNote("CEGIS was killed (assuming user kill); exiting.");
                System.exit(status.exitCode);
            } else if (status.exception != null) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {}
                if (SynchronousTimedProcess.wasKilled.get()) {
                    DebugOut.printNote("CEGIS was killed (assuming user kill); exiting.");
                    System.exit(status.exitCode);
                } else {
                    throw new RuntimeException(status.exception);
                }
            }
        } else if (status.exception instanceof IOException) {
            System.err.println("Warning: lost some output from backend because of timeout.");
            return false;
        }

        lastSolveStats = parseStats(status.out);
        lastSolveStats.success = (0 == status.exitCode) && !status.killedByTimeout;
        log(2, "Stats for last run:\n" + lastSolveStats);

        solverErrorStr = status.err;
        log("Solver exit value: " + status.exitCode);

        return lastSolveStats.success;
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
		return options.debugOpts.verbosity >= 3;
	}

	// TODO: duplication is absurd now, need to use the Logger class
	protected void log (String msg) {  log (3, msg);  }
	protected void log (int level, String msg) {
		if (options.debugOpts.verbosity >= level)
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
