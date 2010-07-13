package sketch.compiler.smt.cvc3;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.StringReader;

import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.dataflow.recursionCtrl.RecursionControl;
import sketch.compiler.main.seq.SMTSketchOptions;
import sketch.compiler.main.seq.SequentialSMTSketchMain;
import sketch.compiler.smt.partialeval.FormulaPrinter;
import sketch.compiler.smt.partialeval.NodeToSmtVtype;
import sketch.compiler.smt.partialeval.SmtValueOracle;
import sketch.compiler.smt.solvers.SMTBackend;
import sketch.compiler.smt.solvers.STPTranslator;
import sketch.compiler.solvers.SolutionStatistics;
import sketch.util.InterceptedOutputStream;
import sketch.util.ProcessStatus;
import sketch.util.Stopwatch;
import sketch.util.SynchronousTimedProcess;

/**
 * A backend class for outputing formulas in CVC3 format
 * @author Lexin Shan
 * @email lshan@eecs.berkeley.edu
 */
public class Cvc3Backend extends SMTBackend {
    protected final static boolean USE_FILE_SYSTEM = true;
    protected SequentialSMTSketchMain sketch;
    String solverOutput;

    public Cvc3Backend(SMTSketchOptions options, String tmpFilePath,
            RecursionControl rcontrol, TempVarGen varGen, boolean tracing)
            throws IOException
    {
        super(options, tmpFilePath, rcontrol, varGen, tracing);
    }

    @Override
    public SolutionStatistics solve(NodeToSmtVtype formula) throws IOException,
            InterruptedException
    {
        try {
            ProcessStatus status;
            Stopwatch watch = new Stopwatch();
            watch.start();
            status = getSolverProcess().run(true); // have to pass true so the
            // output of the solver is
            // not truncated
            watch.stop();
            this.solverOutput = status.out;
            SolutionStatistics stat =
                    (SolutionStatistics) createSolutionStat(status);
            stat.setSolutionTimeMs(watch.toValue());
            
            mOracle = createValueOracle();
            mOracle.linkToFormula(formula);
            if (stat.successful()) {
                LineNumberReader lir =
                        new LineNumberReader(new StringReader(solverOutput));
                getOracle().loadFromStream(lir);
                lir.close();
                return stat;
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return null;
    }

    public OutputStream createStreamToSolver() throws FileNotFoundException {
        if (USE_FILE_SYSTEM) {
            // use file system for input purpose
            File tmpFile = new File(getTmpFilePath());
            OutputStream ret = new FileOutputStream(tmpFile);
            return ret;
        } else {
            OutputStream ret = getSolverProcess().getOutputStream();
            if (this.outputTmpFile) {
                OutputStream fos = new FileOutputStream(this.getTmpFilePath());
                ret = new InterceptedOutputStream(ret, fos);
            }
            if (this.tracing) {
                ret = new InterceptedOutputStream(ret, System.out);
            }
            return ret;
        }
    }

    @Override
    protected SynchronousTimedProcess createSolverProcess() throws IOException {
        String command;
        if (USE_FILE_SYSTEM) {
            command = options.smtOpts.solverpath + " -model " + getTmpFilePath();
        } else {
            command = options.smtOpts.solverpath + " -model ";
        }
        String[] commandLine = command.split(" ");
        return (new SynchronousTimedProcess(options.solverOpts.timeout,
                commandLine));
    }

    protected SolutionStatistics createSolutionStat(ProcessStatus status) {
        return new Cvc3SolutionStatistics(status);
    }

    @Override
    public SmtValueOracle createValueOracle() {
        return new Cvc3Oracle(mTrans);
    }

    @Override
    protected FormulaPrinter createFormulaPrinterInternal(
            NodeToSmtVtype formula, PrintStream ps)
    {
        return new STPTranslator(formula, ps, mIntNumBits);
    }
}
