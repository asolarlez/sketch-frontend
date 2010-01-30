package sketch.compiler.smt.solvers;

import java.io.IOException;
import java.io.PrintStream;

import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.dataflow.recursionCtrl.RecursionControl;
import sketch.compiler.main.seq.SMTSketchOptions;
import sketch.compiler.smt.partialeval.FormulaPrinter;
import sketch.compiler.smt.partialeval.NodeToSmtVtype;
import sketch.compiler.smt.smtlib.SMTLIBTranslatorBV;
import sketch.util.SynchronousTimedProcess;

public class StpSmtlibBackend extends STPBackend {
	private final static boolean USE_FILE_SYSTEM = true;
	
	public StpSmtlibBackend(SMTSketchOptions options, String tmpFilePath,
			RecursionControl rcontrol, TempVarGen varGen, boolean tracing)
			throws IOException {
		super(options, tmpFilePath, rcontrol, varGen, tracing);
	}
	
	@Override
	protected SynchronousTimedProcess createSolverProcess() throws IOException {
		String command;
		if (USE_FILE_SYSTEM) {
			command = options.smtOpts.solverpath + " -m -p -s" + " " + getTmpFilePath();
		} else {
			command = options.smtOpts.solverpath + " -m -p -s"; 
		}
		String[] commandLine = command.split(" ");
		return new SynchronousTimedProcess(options.solverOpts.timeout, commandLine);
	}
	
	@Override
    protected FormulaPrinter createFormulaPrinterInternal(NodeToSmtVtype formula,
            PrintStream ps)
    {
        return new SMTLIBTranslatorBV(formula, ps, mIntNumBits);
    }
}
