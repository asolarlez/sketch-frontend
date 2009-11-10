package sketch.compiler.smt.z3;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.StringReader;

import sketch.compiler.CommandLineParamManager;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.dataflow.recursionCtrl.RecursionControl;
import sketch.compiler.smt.SMTBackend;
import sketch.compiler.smt.SMTTranslator;
import sketch.compiler.smt.SolverFailedException;
import sketch.compiler.smt.partialeval.NodeToSmtVtype;
import sketch.compiler.smt.partialeval.SmtValueOracle;
import sketch.compiler.smt.smtlib.SMTLIBTranslator;
import sketch.compiler.solvers.SolutionStatistics;
import sketch.util.ProcessStatus;
import sketch.util.Stopwatch;
import sketch.util.SynchronousTimedProcess;

public class Z3BVBackend extends SMTBackend {

	String solverInputFile;
	
	public Z3BVBackend(CommandLineParamManager params, String tmpFilePath,
			RecursionControl rcontrol, TempVarGen varGen, boolean tracing) throws IOException {
		super(params, tmpFilePath, rcontrol, varGen, tracing);

	}

	@Override
	public SolutionStatistics solve(NodeToSmtVtype formula) throws IOException, InterruptedException {
		
		Stopwatch watch = new Stopwatch();
		watch.start();
		ProcessStatus run = getSolverProcess().run(true);
		watch.stop();
		
		String solverOutput = run.out;
		String solverError = run.err;
		
		if (solverError.contains("ERROR:")) {
			throw new SolverFailedException(solverOutput + "\n" + solverError);	
		}
		
		Z3SolutionStatistics stat = new Z3SolutionStatistics(run.out, run.err);
		
		mOracle = createValueOracle();
		mOracle.linkToFormula(formula);
		
		if (stat.success) {
			LineNumberReader lir = new LineNumberReader(new StringReader(run.out));
			getOracle().loadFromStream(lir);
			lir.close();
		}
	
		return stat;
	}

	@Override
	protected SMTTranslator createSMTTranslator() {
		return new SMTLIBTranslator(mIntNumBits);
	}

	@Override
	protected SynchronousTimedProcess createSolverProcess() throws IOException {
		String z3Path = this.params.sValue("smtpath");
		SynchronousTimedProcess stp = new SynchronousTimedProcess(params.flagValue("timeout"),
				z3Path, this.getTmpFilePath(), "/m", "/st");
		return stp;
	}

	@Override
	protected OutputStream createStreamToSolver() throws IOException {
		FileOutputStream fos = new FileOutputStream(this.getTmpFilePath());
		return fos;
	}

	@Override
	protected SmtValueOracle createValueOracle() {
		return new Z3ManualParseOracle2_0();
	}

}
