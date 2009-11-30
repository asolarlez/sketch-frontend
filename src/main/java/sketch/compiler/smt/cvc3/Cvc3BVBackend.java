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

/**
 * Backend used for emitting Cvc3 BitVector formulas
 * 
 * @author Lexin Shan
 * @email lshan@eecs.berkeley.edu
 *
 */
public class Cvc3BVBackend extends Cvc3Backend{

	public Cvc3BVBackend(CommandLineParamManager params, String tmpFilePath,
			RecursionControl rcontrol, TempVarGen varGen, boolean tracing)
			throws IOException {
		super(params, tmpFilePath, rcontrol, varGen, tracing);
	}

	@Override
    protected FormulaPrinter createFormulaPrinterInternal(NodeToSmtVtype formula,
            PrintStream ps)
    {
        return new SMTLIBTranslator(formula, ps, mIntNumBits);
    }
	
	@Override
	protected SmtValueOracle createValueOracle() {
		return new Cvc3BVOracle(mTrans);
	}
	
}
