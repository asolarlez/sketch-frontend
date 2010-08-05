package sketch.compiler.smt;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.PrintStream;

import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.dataflow.recursionCtrl.RecursionControl;
import sketch.compiler.main.seq.SMTSketchOptions;
import sketch.compiler.smt.partialeval.FormulaPrinter;
import sketch.compiler.smt.partialeval.NodeToSmtVtype;
import sketch.compiler.smt.partialeval.SmtValueOracle;
import sketch.compiler.smt.smtlib.SMTLIBTranslatorBV;
import sketch.compiler.smt.solvers.SMTBackend;
import sketch.compiler.solvers.SolutionStatistics;
import sketch.compiler.solvers.constructs.AbstractValueOracle;
import sketch.util.InterceptedOutputStream;
import sketch.util.NullStream;
import sketch.util.ProcessStatus;
import sketch.util.SynchronousTimedProcess;

public class FakeSolverBackend extends SMTBackend {
	
	public class DummyProcess extends SynchronousTimedProcess {

		public DummyProcess(int mins, String[] cmdLine) throws IOException {
			super(mins, cmdLine);
		}

		@Override
		public ProcessStatus run(boolean logAllOutput) {
			ProcessStatus st = new ProcessStatus();
			st.err = "";
			st.out = "";
			return st;
		}
	}
	
	public class FakeOracle extends SmtValueOracle {

		public FakeOracle() {
			super(mTrans);
		}

		@Override
		public void loadFromStream(LineNumberReader in) throws IOException {
		}

		@Override
		public ExprConstInt popValueForNode(FENode node) {
			return null;
		}
		
	}

	private AbstractValueOracle oracle;
	private OutputStream outputStreamToSolver;

	public FakeSolverBackend(SMTSketchOptions options,
			String tmpFilePath, RecursionControl rcontrol, TempVarGen varGen, boolean tracing) throws IOException {
		super(options, tmpFilePath, rcontrol, varGen, tracing);
		outputStreamToSolver = getOutputStreamToSolver();
	}

	
	protected SolutionStatistics createSolutionStat() {
		return new SolutionStatistics() {
			@Override
			public boolean successful() {
				return false;
			}

			@Override
			public long elapsedTimeMs() {
				return 0;
			}

			@Override
			public long maxMemoryUsageBytes() {
				return 0;
			}

			@Override
			public long modelBuildingTimeMs() {
				return 0;
			}

			@Override
			public long solutionTimeMs() {
				return 0;
			}
		};
	}

	
	protected SynchronousTimedProcess createSolverProcess() throws IOException {
		String[] dummy = {"clear"};
		return new DummyProcess(1, dummy);
	}

	@Override
	public SmtValueOracle createValueOracle() {
		return new FakeOracle();
	}
	
	@Override
	public OutputStream createStreamToSolver() throws IOException {
		OutputStream ret = new NullStream();
		if (this.outputTmpFile) {
			System.out.println("Formulas output to: " + this.getTmpFilePath());
			ret = new FileOutputStream(this.getTmpFilePath());
		}
		
		if (this.tracing) {
			ret = new InterceptedOutputStream(ret, System.out);
		}
		return ret;
	}
	


	@Override
	public SolutionStatistics solve(NodeToSmtVtype formula) throws IOException,
			InterruptedException, SolverFailedException {
		return createSolutionStat();
	}


    @Override
    public FormulaPrinter createFormulaPrinterInternal(NodeToSmtVtype formula,
            PrintStream ps)
    {
        return new SMTLIBTranslatorBV(formula, ps, mIntNumBits);
    }
	
	

	

}
