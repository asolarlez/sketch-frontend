package sketch.compiler.smt.beaver;

import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.OutputStream;

import sketch.compiler.CommandLineParamManager;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.dataflow.recursionCtrl.RecursionControl;
import sketch.compiler.smt.SMTBackend;
import sketch.compiler.smt.SMTTranslator;
import sketch.compiler.smt.SolverFailedException;
import sketch.compiler.smt.partialeval.NodeToSmtVtype;
import sketch.compiler.smt.partialeval.SmtValueOracle;
import sketch.compiler.solvers.SolutionStatistics;
import sketch.util.InterceptedOutputStream;
import sketch.util.ProcessStatus;
import sketch.util.SynchronousTimedProcess;

public class BeaverBVBackend extends SMTBackend {
	
	public BeaverBVBackend(CommandLineParamManager params, String tmpFilePath,
			RecursionControl rcontrol, TempVarGen varGen, boolean tracing) throws IOException {
		super(params, tmpFilePath, rcontrol, varGen, tracing);
	}

	@Override
	protected SMTTranslator createSMTTranslator() {
		return new BeaverSMTLIBTranslator(mIntNumBits);
	}

	@Override
	protected SynchronousTimedProcess createSolverProcess() throws IOException {
		// -m tells Beaver to output the model, and --model-file tells where to put it
		String command = params.sValue("smtpath") + " -m" + " --model-file=" + getModelFilename();
		String[] commandLine = command.split(" ");
		return new SynchronousTimedProcess(params.flagValue("timeout"), commandLine);
	}

	@Override
	protected OutputStream createStreamToSolver() throws IOException {
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
	protected SmtValueOracle createValueOracle() {
		return new BeaverBVOracle();
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


}
