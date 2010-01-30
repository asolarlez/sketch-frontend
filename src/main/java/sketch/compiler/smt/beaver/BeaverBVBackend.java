package sketch.compiler.smt.beaver;

import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.PrintStream;

import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.dataflow.recursionCtrl.RecursionControl;
import sketch.compiler.main.seq.SMTSketchOptions;
import sketch.compiler.smt.SolverFailedException;
import sketch.compiler.smt.partialeval.FormulaPrinter;
import sketch.compiler.smt.partialeval.NodeToSmtVtype;
import sketch.compiler.smt.partialeval.SmtValueOracle;
import sketch.compiler.smt.solvers.SMTBackend;
import sketch.compiler.solvers.SolutionStatistics;
import sketch.util.InterceptedOutputStream;
import sketch.util.ProcessStatus;
import sketch.util.SynchronousTimedProcess;

public class BeaverBVBackend extends SMTBackend {

	public BeaverBVBackend(SMTSketchOptions options, String tmpFilePath,
			RecursionControl rcontrol, TempVarGen varGen, boolean tracing) throws IOException {
		super(options, tmpFilePath, rcontrol, varGen, tracing);
	}

	@Override
	protected SynchronousTimedProcess createSolverProcess() throws IOException {
		// -m tells Beaver to output the model, and --model-file tells where to put it
		String command = options.smtOpts.solverpath + " -m" + " --model-file=" + getModelFilename();
		String[] commandLine = command.split(" ");
		return new SynchronousTimedProcess(options.solverOpts.timeout, commandLine);
	}

	@Override
	public OutputStream createStreamToSolver() throws IOException {
		// Copy-pasted from Cvc3Backend
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

	@Override
	public SmtValueOracle createValueOracle() {
		return new BeaverBVOracle(mTrans);
	}
	
	private String getModelFilename() {
		return getTmpFilePath() + ".model";
	}

	@Override
	public SolutionStatistics solve(NodeToSmtVtype formula) throws IOException,
			InterruptedException, SolverFailedException {
		ProcessStatus run = getSolverProcess().run(true);
		String solverOutput = run.out;
		LineNumberReader lir = new LineNumberReader(new FileReader(getModelFilename()));
		getOracle().loadFromStream(lir);
		lir.close();
		return new BeaverSolutionStatistics(solverOutput);
	}

    @Override
    public FormulaPrinter createFormulaPrinterInternal(NodeToSmtVtype formula,
            PrintStream ps)
    {
        return new BeaverSMTLIBTranslator(formula, ps, mIntNumBits);
    }


}
