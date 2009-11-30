package sketch.compiler.smt.cvc3;

import java.io.IOException;
import java.io.PrintStream;

import sketch.compiler.CommandLineParamManager;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.dataflow.recursionCtrl.RecursionControl;
import sketch.compiler.smt.partialeval.FormulaPrinter;
import sketch.compiler.smt.partialeval.NodeToSmtVtype;
import sketch.compiler.smt.partialeval.SmtValueOracle;
import sketch.compiler.smt.smtlib.SMTLIBTranslator;
import sketch.compiler.solvers.SolutionStatistics;
import sketch.util.ProcessStatus;
import sketch.util.SynchronousTimedProcess;

/**
 * This backend class is for using CVC3 in SMTLIB format.
 * @author Lexin Shan
 * @email lshan@eecs.berkeley.edu
 *
 */
public class Cvc3SMTLIBBackend extends Cvc3Backend {

	public Cvc3SMTLIBBackend(CommandLineParamManager params, String tmpFilePath,
			RecursionControl rcontrol, TempVarGen varGen, boolean tracing)
			throws IOException {
		super(params, tmpFilePath, rcontrol, varGen, tracing);
	}
	
	@Override
	protected SynchronousTimedProcess createSolverProcess() throws IOException {
		String command;
		
		if (Cvc3Backend.USE_FILE_SYSTEM) {
			command = params.sValue("smtpath") + " -lang smtlib" + " " + getTmpFilePath();
		} else {
			command = params.sValue("smtpath") + " -lang smtlib";
		}
		String[] commandLine = command.split(" ");
		return (new SynchronousTimedProcess(params
				.flagValue("timeout"), commandLine));
	}
	
	@Override
	protected SmtValueOracle createValueOracle() {
		return new SMTLIBOracle(mTrans);
	}
	
	@Override
	protected SolutionStatistics createSolutionStat(ProcessStatus status) {
		return new Cvc3SMTLIBSolutionStatistics(status);
	}
	
	@Override
    protected FormulaPrinter createFormulaPrinterInternal(NodeToSmtVtype formula,
            PrintStream ps)
    {
        return new SMTLIBTranslator(formula, ps, mIntNumBits);
    }
	
	
}
