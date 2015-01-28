package sketch.compiler.solvers;

import java.io.*;
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
import sketch.compiler.solvers.constructs.RandomValueOracle;
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

public class SATBackend {

    String solverErrorStr;
    final RecursionControl rcontrol;
    protected final TempVarGen varGen;
    protected ValueOracle oracle;
    private boolean tracing = false;
    private SATSolutionStatistics lastSolveStats;
    public final SketchOptions options;
    protected boolean minimize = false;

    public SATBackend(SketchOptions options,
            RecursionControl rcontrol, TempVarGen varGen)
    {
        this.options = options;
        this.rcontrol = rcontrol;
        this.varGen = varGen;
    }

    public void activateTracing(){
        tracing = true;
    }

    @SuppressWarnings("unchecked")
    public String[] getBackendCommandline(int i, Vector<String> commandLineOptions_,
            String... additional)
    {
        Vector<String> commandLineOptions = (Vector<String>) commandLineOptions_.clone();
        PlatformLocalization pl = PlatformLocalization.getLocalization();
        String cegisScript = pl.getCegisPath();
        commandLineOptions.insertElementAt(cegisScript, 0);

        if (options.solverOpts.parallel) {
            commandLineOptions.add("--seed");
            // negative integer, e.g., "-1", may look like an option
            int abs_seed = Math.abs(options.solverOpts.seed + i);
            commandLineOptions.add("" + abs_seed);
        }

        if (options.solverOpts.randdegree > 0) {
            commandLineOptions.add("-randdegree");
            commandLineOptions.add("" + options.solverOpts.randdegree);
        }

        commandLineOptions.add("-o");
        commandLineOptions.add(options.getSolutionsString(i));
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
        PrintStream pstream = new PrintStream(outStream, false);
        sketch.compiler.dataflow.nodesToSB.ProduceBooleanFunctions partialEval =
                new sketch.compiler.dataflow.nodesToSB.ProduceBooleanFunctions(varGen,
                        oracle,
                        pstream
                        // System.out
                        , options.bndOpts.unrollAmnt, options.bndOpts.arrSize , rcontrol, tracing);
        log("MAX LOOP UNROLLING = " + options.bndOpts.unrollAmnt);
        log("MAX FUNC INLINING  = " + options.bndOpts.inlineAmnt);
        
        prog.accept(partialEval);

        pstream.flush();
        log("After prog.accept(partialEval)");
    }

    public Program preprocess(Program prog) {
        final HasMinimize hasMinimize = new HasMinimize();
        hasMinimize.visitProgram(prog);
        if (hasMinimize.hasMinimize()) {
            minimize = true;
            final AbstractCostFcnAssert costFcnAssert =
                    new AbstractCostFcnAssert(options.bndOpts.mbits);
            return (Program) costFcnAssert.visitProgram(prog);
        } else {
            return prog;
        }
    }

    // will be overridden to run the back-end differently, e.g., in parallel
    protected boolean solve(ValueOracle oracle, boolean hasMinimize, float timeoutMins) {
        SATSolutionStatistics stat = null;
        try {
            stat = incrementalSolve(oracle, minimize, timeoutMins);
        } catch (SketchSolverException e) {
            e.setBackendTempPath(options.getTmpSketchFilename());
        }
        return stat != null && stat.success;
    }

    public boolean partialEvalAndSolve(Program prog) {
        // prog.debugDump("Program before solving");
        oracle = new ValueOracle(new StaticHoleTracker(varGen));
        log("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
        // prog.accept(new SimpleCodePrinter());
        assert oracle != null;

        boolean worked = false;
        if (options.debugOpts.fakeSolver) {
            worked = true;
        } else {
            options.cleanTemp();
            writeProgramToBackendFormat(preprocess(prog));
            worked = solve(oracle, minimize, options.solverOpts.timeout);
        }

        if (!worked && !options.feOpts.forceCodegen) {
            throw new SketchNotResolvedException(options.getTmpSketchFilename(),
                    this.solverErrorStr);
        }

        {
            java.io.File fd = new File(options.getTmpSketchFilename());
            if (fd.exists() && !(options.feOpts.keepTmp || options.debugOpts.fakeSolver))
            {
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
            if (options.feOpts.forceCodegen || options.debugOpts.fakeSolver) {
                oracle = new RandomValueOracle(new StaticHoleTracker(varGen));
                System.err.println("***********************************************************************");
                System.err.println("*WARNING: The system is generating unchecked and possibly buggy code***");
                System.err.println("***********************************************************************");
            } else {
                assertFalse("No solutions found in folder", options.sktmpdir());
            }
            return worked;
        }
        extractOracleFromOutput(solutions[0].getPath());
        if (!(options.feOpts.keepTmp || options.debugOpts.fakeSolver)) {
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
            SATSolutionStatistics currResult = incrementalSolve(oracle, false, timeout);
            worked |= currResult.success;
            if (!currResult.success) {
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
                outStream =
                        new BufferedOutputStream(new FileOutputStream(
                                options.getTmpSketchFilename()), 4096);
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
        try {
            File f = new File(fname);
            FileInputStream fis = new FileInputStream(f);
            BufferedInputStream bis = new BufferedInputStream(fis);
            LineNumberReader lir = new LineNumberReader(new InputStreamReader(bis));
            oracle.loadFromStream(lir);
            fis.close();
            java.io.File fd = new File(fname);
            if (fd.exists() && !(options.feOpts.keepTmp || options.debugOpts.fakeSolver))
            {
                fd.delete();
            }
        }
        catch (java.io.IOException e)
        {
            //e.printStackTrace(System.err);
            throw new RuntimeException(e);
        }
    }

    protected SATSolutionStatistics incrementalSolve(ValueOracle oracle,
            boolean hasMinimize,
            float timeoutMins)
    {
        return incrementalSolve(oracle, hasMinimize, timeoutMins, 0);
    }

    protected SATSolutionStatistics incrementalSolve(ValueOracle oracle,
            boolean hasMinimize,
            float timeoutMins, int fileIdx)
    {
        Vector<String> backendOptions = options.getBackendOptions();
        log("OFILE = " + options.feOpts.output);
        SATSolutionStatistics ret = null;

        // minimize
        if (options.bndOpts.incremental.isSet) {
            boolean isSolved = false;
            int bits = 0;
            int maxBits = options.bndOpts.incremental.value;
            for (bits = 1; bits <= maxBits; ++bits) {
                log("TRYING SIZE " + bits);
                String[] commandLine = getBackendCommandline(fileIdx, backendOptions, "--bnd-cbits=" + bits);
                ret = runSolver(commandLine, bits, timeoutMins);
                if (ret != null && ret.success) {
                    isSolved = true;
                    break;
                } else {
                    log("Size " + bits + " is not enough");
                }
            }
            if (isSolved) {
                log("Succeded with " + bits + " bits for integers");
                oracle.capStarSizes(bits);
            }
        }
        // default
        else {
            String[] commandLine;
            if (hasMinimize) {
                commandLine = getBackendCommandline(fileIdx, backendOptions, "--minvarHole");
            } else {
                commandLine = getBackendCommandline(fileIdx, backendOptions);
            }
            ret = runSolver(commandLine, 0, timeoutMins);
        }
        if (ret != null && !ret.success) {
            log(5, "The sketch cannot be resolved.");
            // System.err.println(solverErrorStr);
        }
        return ret;
    }

    protected void logCmdLine(String[] commandLine){
        String cmdLine = "";
        for (String a : commandLine)  cmdLine += a + " ";
        log ("Launching: "+ cmdLine);
    }

    // will be overridden by parallel version
    protected void beforeRunSolver(SynchronousTimedProcess proc) {
        // utilize CEGIS process information here
    }

    private SATSolutionStatistics runSolver(String[] commandLine, int bits,
            float timeoutMins)
    {
        logCmdLine(commandLine);

        SynchronousTimedProcess proc;
        try {
            proc = new SynchronousTimedProcess(timeoutMins, commandLine);
        } catch (IOException e) {
            throw new SketchSolverException(
                    "Could not instantiate solver (CEGIS) process.", e);
        }

        beforeRunSolver(proc);
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
            SATSolutionStatistics err_stat = new SATSolutionStatistics();
            if (options.solverOpts.parallel) {
                err_stat.elapsedTimeMs = (long) (options.solverOpts.pTimeout * 60 * 1000);
            }
            err_stat.success = false;
            return err_stat;
        }

        SATSolutionStatistics be_stat = parseStats(status.out);
        be_stat.success = (0 == status.exitCode) && !status.killedByTimeout;
        lastSolveStats = be_stat;
        log(2, "Stats for last run:\n" + lastSolveStats);

        solverErrorStr = status.err;
        log("Solver exit value: " + status.exitCode);

        return be_stat;
    }

    protected SATSolutionStatistics parseStats (String out) {
        SATSolutionStatistics s = new SATSolutionStatistics ();
        s.out = out;

        List<String> res;
        String NL = "(?:\\r\\n|\\n|\\r)";

        // XXX: using max virtual mem; maybe resident or private is better
        res = Misc.search (out,
                "Total elapsed time \\(ms\\):\\s+(\\d+(?:\\.\\d+)?)"+ NL +
                "Model building time \\(ms\\):\\s+(\\d+(?:\\.\\d+)?)"+ NL +
                "Solution time \\(ms\\):\\s+(\\d+(?:\\.\\d+)?)"+ NL +
                "Max virtual mem \\(bytes\\):\\s+(\\d+)");
        if (res != null) {
            s.elapsedTimeMs = (long) (Float.parseFloat (res.get (0)));
            s.modelBuildingTimeMs = (long) (Float.parseFloat (res.get (1)));
            s.solutionTimeMs = (long) (Float.parseFloat (res.get (2)));
            s.maxMemUsageBytes = Long.parseLong (res.get (3));
        } else {
            s.elapsedTimeMs = -1;
            s.modelBuildingTimeMs = -1;
            s.solutionTimeMs = -1;
            s.maxMemUsageBytes = -1;
        }

        // even failed case, we need elapsed time
        if (s.elapsedTimeMs < 0) {
            res = Misc.search(out, "FIND TIME \\S+ CHECK TIME \\S+ TOTAL TIME (\\S+)");
            if (res != null) {
                s.elapsedTimeMs = 0;
                for (int i = 0; i < res.size(); i++) {
                    s.elapsedTimeMs += (long) (Float.parseFloat(res.get(i)));
                }
            }
        }

        // if it's still below 0, this means (short) time out trial
        if (s.elapsedTimeMs <= 0) {
            s.elapsedTimeMs = (long) (options.solverOpts.pTimeout * 60 * 1000);
        }

        res = Misc.search (out, "SKETCH nodes = (\\d+)");
        if (res != null) {
            s.numNodesInitial = Long.parseLong (res.get (0));
        } else {
            s.numNodesInitial = -1;
        }

        res = Misc.search (out, "Final Problem size: Problem nodes = (\\d+)");
        if (null != res) {
            s.numNodesFinal = Long.parseLong (res.get (0));
        } else {
            s.numNodesFinal = -1;
        }

        res = Misc.search (out, "# OF CONTROLS:\\s+(\\d+)");
        if (null != res) {
            s.numControls = Long.parseLong (res.get (0));
        } else {
            s.numControls = -1;
        }

        res = Misc.search (out, "ctrlSize = (\\d+)");
        if (null != res) {
            s.numControlBits = Long.parseLong (res.get (0));
        } else {
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
    protected void log(String msg) {
        log(3, msg);
    }

    protected void plog(String msg) {
        plog(System.out, msg);
    }

    protected void plog(PrintStream out, String msg) {
        log(out, options.debugOpts.verbosity, msg);
    }

    protected void log(int level, String msg) {
        log(System.out, level, msg);
    }

    protected void log(PrintStream out, int level, String msg) {
        if (options.debugOpts.verbosity >= level)
            out.println("[SATBackend] " + msg);
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
