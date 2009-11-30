package sketch.compiler.smt.cvc3;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.StringReader;

import sketch.compiler.CommandLineParamManager;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.dataflow.recursionCtrl.RecursionControl;
import sketch.compiler.main.seq.SequentialSMTSketchMain;
import sketch.compiler.smt.SMTBackend;
import sketch.compiler.smt.partialeval.FormulaPrinter;
import sketch.compiler.smt.partialeval.NodeToSmtVtype;
import sketch.compiler.smt.partialeval.SmtValueOracle;
import sketch.compiler.smt.smtlib.SMTLIBTranslator;
import sketch.compiler.solvers.SolutionStatistics;
import sketch.util.InterceptedOutputStream;
import sketch.util.ProcessStatus;
import sketch.util.Stopwatch;
import sketch.util.SynchronousTimedProcess;

/**
 * A backend class for outputing formulas in CVC3 format
 * @author Lexin Shan
 * @email lshan@eecs.berkeley.edu
 *
 */
public class Cvc3Backend extends SMTBackend {
	
	protected final static boolean USE_FILE_SYSTEM = true;
	
	protected SequentialSMTSketchMain sketch;

	
	String solverOutput;
	
	public Cvc3Backend(CommandLineParamManager params, String tmpFilePath,
			RecursionControl rcontrol, TempVarGen varGen, boolean tracing) throws IOException {
		super(params, tmpFilePath, rcontrol, varGen, tracing);		
	}
	
	@Override
	public SolutionStatistics solve(NodeToSmtVtype formula) throws IOException, InterruptedException {

		try {
			ProcessStatus status;
			Stopwatch watch = new Stopwatch();
			watch.start();
			status = getSolverProcess().run(true); // have to pass true so the output of the solver is not truncated
			watch.stop();
			this.solverOutput = status.out;
			
			SolutionStatistics stat = (SolutionStatistics) createSolutionStat(status);
			
			stat.setSolutionTimeMs(watch.toValue());
			
			if (!stat.successful()) {
				log(3, "Solver error:");
				log(3, status.err);
				log(3, status.out);
				return stat;
			} else {
				if (tracing) {
					System.out.println(status.out);
					System.err.println(status.err);
				}
			
				mOracle.linkToFormula(formula);
				
				LineNumberReader lir = new LineNumberReader(new StringReader(solverOutput));
				getOracle().loadFromStream(lir);
				lir.close();
				
				return stat;
			}

		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (InterruptedException e) {
			e.printStackTrace();
			System.exit(1);
		}

		return null;
	}

	protected OutputStream createStreamToSolver() throws FileNotFoundException {
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

		String command = params.sValue("smtpath");
		String[] commandLine = command.split(" ");
		return (new SynchronousTimedProcess(params
				.flagValue("timeout"), commandLine));

	}
	
	protected SolutionStatistics createSolutionStat(ProcessStatus status) {
		return new Cvc3SolutionStatistics(status);
	}

	@Override
	protected SmtValueOracle createValueOracle() {
		return new Cvc3Oracle(mTrans);
	}

	@Override
    protected FormulaPrinter createFormulaPrinterInternal(NodeToSmtVtype formula,
            PrintStream ps)
    {
        return new SMTLIBTranslator(formula, ps, mIntNumBits);
    }

}
