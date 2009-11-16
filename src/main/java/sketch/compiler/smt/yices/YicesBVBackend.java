package sketch.compiler.smt.yices;

import java.io.File;
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
import sketch.util.InterceptedOutputStream;
import sketch.util.ProcessStatus;
import sketch.util.Stopwatch;
import sketch.util.SynchronousTimedProcess;

public class YicesBVBackend extends SMTBackend {
	
    private final static String V1_SWICHES = " -smt -e -tc -st ";
    private final static String V2_SWICHES = " -f ";
    private String mSwitch;
	private final static boolean USE_FILE_SYSTEM = true;
	
	public YicesBVBackend(CommandLineParamManager params, String tmpFilePath,
			RecursionControl rcontrol, TempVarGen varGen, int version, boolean tracing) throws IOException {
		super(params, tmpFilePath, rcontrol, varGen, tracing);
		if (version == 1)
		    mSwitch = V1_SWICHES;
		else
		    mSwitch = V2_SWICHES;
	}

	@Override
	public SolutionStatistics solve(NodeToSmtVtype formula) throws IOException, InterruptedException {
		Stopwatch watch = new Stopwatch();
		watch.start();
		ProcessStatus run = getSolverProcess().run(true);
		watch.stop();
		
		String solverOutput = run.out;
		String solverError = run.err;
		
		YicesSolutionStatistics stat = new YicesSolutionStatistics(solverOutput, solverError);
		
		mOracle = createValueOracle();
		mOracle.linkToFormula(formula);
		
		if (stat.successful()) {
			LineNumberReader lir = new LineNumberReader(new StringReader(solverOutput));
			mOracle.loadFromStream(lir);
			lir.close();
		}
		
		if (!solverError.equals(""))
			throw new SolverFailedException(solverError);
		
		return stat;
	}

	@Override
	protected SMTTranslator createSMTTranslator() {
		return new SMTLIBTranslator(mIntNumBits);
	}

	@Override
	protected SynchronousTimedProcess createSolverProcess() throws IOException {
		// -smt tells yices the input is in smt format, and -e tells yices to output the model
		String command;
		if (USE_FILE_SYSTEM) {
			command = params.sValue("smtpath") + mSwitch + getTmpFilePath();
		} else {
			command = params.sValue("smtpath") + mSwitch; 
		}
		String[] commandLine = command.split(" ");
		return new SynchronousTimedProcess(params.flagValue("timeout"), commandLine);
	}

	@Override
	protected OutputStream createStreamToSolver() throws IOException {
		
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
	protected SmtValueOracle createValueOracle() {
		return new YicesBVOracle();
	}



}
