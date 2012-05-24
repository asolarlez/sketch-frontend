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
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import org.apache.commons.io.FileUtils;

import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.dataflow.recursionCtrl.RecursionControl;
import sketch.compiler.main.PlatformLocalization;
import sketch.compiler.main.cmdline.SketchOptions;
import sketch.compiler.passes.optimization.AbstractCostFcnAssert;
import sketch.compiler.passes.optimization.CostFcnAssert;
import sketch.compiler.passes.structure.HasMinimize;
import sketch.compiler.solvers.constructs.StaticHoleTracker;
import sketch.compiler.solvers.constructs.ValueOracle;
import sketch.util.Misc;
import sketch.util.NullStream;
import sketch.util.ProcessStatus;
import sketch.util.SynchronousTimedProcess;
import sketch.util.datastructures.IntRange;
import sketch.util.exceptions.SketchNotResolvedException;
import sketch.util.exceptions.SketchSolverException;
import sketch.util.wrapper.ScRichString;

import static sketch.util.DebugOut.assertFalse;
import static sketch.util.DebugOut.printDebug;
import static sketch.util.DebugOut.printNote;

public class SATBackend {

    String solverErrorStr;
	final RecursionControl rcontrol;
	final TempVarGen varGen;
	protected ValueOracle oracle;
	private boolean tracing = false;
	private SATSolutionStatistics lastSolveStats;
    public final SketchOptions options;

	public SATBackend(SketchOptions options, 
	        RecursionControl rcontrol, TempVarGen varGen)
	{
		this.options = options;
		this.rcontrol =rcontrol;
		this.varGen = varGen;
	}

	public void activateTracing(){
		tracing = true;
	}
	
	public String[] getBackendCommandline(Vector<String> commandLineOptions_, String... additional){
        Vector<String> commandLineOptions = (Vector<String>) commandLineOptions_.clone();
	    PlatformLocalization pl = PlatformLocalization.getLocalization();
        String cegisScript = pl.getCegisPath();
        commandLineOptions.insertElementAt(cegisScript, 0);
        commandLineOptions.add("-o");
        commandLineOptions.add(options.getSolutionsString());
        commandLineOptions.addAll(Arrays.asList(additional));
        commandLineOptions.add(options.getTmpSketchFilename());
        String[] result = commandLineOptions.toArray(new String[0]);
        if (options.debugOpts.verbosity > 4) {
            String cmdLine = (new ScRichString(" ")).join(result);
            printDebug("executing", cmdLine);
        }
        return result;
    }

    protected void partialEval(Program prog, OutputStream outStream) {
        sketch.compiler.dataflow.nodesToSB.ProduceBooleanFunctions partialEval =
                new sketch.compiler.dataflow.nodesToSB.ProduceBooleanFunctions(varGen,
                        oracle, new PrintStream(outStream)
                        // System.out
                        , options.bndOpts.unrollAmnt, options.bndOpts.arrSize , rcontrol, tracing);
        log("MAX LOOP UNROLLING = " + options.bndOpts.unrollAmnt);
        log("MAX FUNC INLINING  = " + options.bndOpts.inlineAmnt);
        
        prog.accept(partialEval);
        log("After prog.accept(partialEval)");
    }

    public boolean partialEvalAndSolve(Program prog) {
        // prog.debugDump("Program before solving");
        oracle = new ValueOracle(new StaticHoleTracker(varGen));
        log("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
        // prog.accept(new SimpleCodePrinter());
        assert oracle != null;

        final HasMinimize hasMinimize = new HasMinimize();
        hasMinimize.visitProgram(prog);
        options.cleanTemp();

        boolean worked = false;
        if (options.debugOpts.fakeSolver) {
            worked = true;
        } else if (hasMinimize.hasMinimize()) {
            if (options.feOpts.minimize) {
                assert false : "deprecated";
                // use the frontend
//                bestValueFile = new File(tmpSketchFilename + ".best");
//                worked = frontendMinimize(prog, sketchOutputFile, bestValueFile, worked);
            } else {
                // use the backend
                printNote("enabling scripting backend due to presence of minimize()");
                options.solverOpts.useScripting = true;
                final AbstractCostFcnAssert costFcnAssert = new AbstractCostFcnAssert();
                writeProgramToBackendFormat((Program) costFcnAssert.visitProgram(prog));
                try {
                    worked = solve(oracle, true, options.solverOpts.timeout);
                } catch (SketchSolverException e) {
                    e.setBackendTempPath(options.getTmpSketchFilename());
                }
            }
        } else {
            writeProgramToBackendFormat(prog);
            try {
                worked = solve(oracle, false, options.solverOpts.timeout);
            } catch (SketchSolverException e) {
                e.setBackendTempPath(options.getTmpSketchFilename());
            }
        }

        if (!worked && !options.feOpts.forceCodegen) {
            throw new SketchNotResolvedException(options.getTmpSketchFilename());
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

        File[] solutions = options.getSolutionsFiles();
        if (solutions.length == 0) {
            assertFalse("No solutions found in folder", options.sktmpdir());
        }
        extractOracleFromOutput(solutions[0].getPath());
        if (!options.feOpts.keepTmp) {
            options.cleanTemp();
        }
        return worked;
    }

    protected boolean frontendMinimize(Program prog, File sketchOutputFile,
            File bestValueFile, boolean worked)
    {
        IntRange currRange = IntRange.inclusive(0, 2 * options.bndOpts.costEstimate);
        float timeout = Math.max(1.f, options.solverOpts.timeout) / ((float) (1 << 10));
        for (int a = 0; a < 100 && !currRange.isEmpty(); a++) {
            // choose a value from not-yet-inspected values
            final int currValue = (int) currRange.middle();
            System.err.println("current: " + currValue + " \\in " + currRange);
            log(2, "current range: " + currValue + " \\in " + currRange);

            // rewrite the minimize statements in the program
            final CostFcnAssert costFcnAssert = new CostFcnAssert(currValue);
            writeProgramToBackendFormat((Program) costFcnAssert.visitProgram(prog));

            // actually run the solver
            boolean currResult = solve(oracle, false, timeout);
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
            assert (new File(options.getTmpSketchFilename())).isFile() : "didn't appear to write file";
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



	
    private boolean solve(ValueOracle oracle, boolean hasMinimize, float timeoutMins) {
        Vector<String> backendOptions = options.getBackendOptions();
        log("OFILE = " + options.feOpts.output);

        int rangeStart = options.bndOpts.intRange0;
        int rangeMax = options.bndOpts.intRange;
        if (rangeMax < rangeStart) {
            rangeMax = rangeStart;
        }

        // minimize
        if (hasMinimize) {
            String[] commandLine =
                    getBackendCommandline(backendOptions, "--use-minimize");
            boolean ret = runSolver(commandLine, 0, timeoutMins);
            // for (int a = rangeStart; a <= rangeMax; a *= 2) {
            // String[] commandLine = getBackendCommandline(backendOptions,
            // "--use-minimize", "--bnd-int-range", a + "");
            // ret = runSolver(commandLine, 0, timeoutMins);
            // if (ret) {
            // break;
            // } else if (2 * a <= rangeMax) {
            // printDebug("Trying next int range bound", 2 * a);
            // }
            // }

            if (!ret) {
                log(5, "Backend returned error code");
                // System.err.println(solverErrorStr);
                return false;
            }

        // TODO -- move incremental to Python backend
        } else if (options.bndOpts.incremental.isSet) {
			boolean isSolved = false;
			int bits=0;
			int maxBits = options.bndOpts.incremental.value;
			for(bits=1; bits<=maxBits; ++bits){
				log ("TRYING SIZE " + bits);			
                String[] commandLine =
                        getBackendCommandline(backendOptions, "--bnd-cbits=" + bits);
				boolean ret = runSolver(commandLine, bits, timeoutMins);
				if(ret){
					isSolved = true;
					break;
				}else{
					log ("Size " + bits + " is not enough");
				}
			}
			if(!isSolved){
				log (5, "The sketch cannot be resolved.");
				// System.err.println(solverErrorStr);
				return false;
			}
			log ("Succeded with " + bits + " bits for integers");
			oracle.capStarSizes(bits);

		// default
        } else {
            String[] commandLine = getBackendCommandline(backendOptions);
            boolean ret = runSolver(commandLine, 0, timeoutMins);
            // for (int a = rangeStart; a <= rangeMax; a *= 2) {
            // String[] commandLine = getBackendCommandline(backendOptions,
            // "--bnd-int-range", a + "");
            // ret = runSolver(commandLine, 0, timeoutMins);
            // if (ret) {
            // break;
            // } else if (2 * a <= rangeMax) {
            // printDebug("Trying next int range bound", 2 * a);
            // }
            // }

            if (!ret) {
                log(5, "Backend returned error code");
                // System.err.println(solverErrorStr);
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
            throw new SketchSolverException(
                    "Could not instantiate solver (CEGIS) process.", e);
        }

        final ProcessStatus status = proc.run(false);

        // deal with killed states
        if (!status.killedByTimeout) {
            if (status.exitCode != 0 && status.err.contains("I've been killed.")) {
                throw new SketchSolverException(
                        "CEGIS was killed (assuming user kill); exiting.");
            } else if (status.exception != null) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {}
                if (SynchronousTimedProcess.wasKilled.get()) {
                    throw new SketchSolverException(
                            "CEGIS was killed (assuming user kill); exiting.");
                } else {
                    throw new RuntimeException(status.exception);
                }
            } else if (status.exitCode == 103) {
                throw new SketchSolverException("CEGIS ran out of memory");
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
