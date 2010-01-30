package sketch.compiler.smt.cvc3;

import java.io.IOException;
import java.io.PrintStream;

import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.dataflow.recursionCtrl.RecursionControl;
import sketch.compiler.main.seq.SMTSketchOptions;
import sketch.compiler.smt.partialeval.FormulaPrinter;
import sketch.compiler.smt.partialeval.NodeToSmtVtype;
import sketch.compiler.smt.partialeval.SmtValueOracle;

/**
 * Backend used for emitting Cvc3 BitVector formulas
 * 
 * @author Lexin Shan
 * @email lshan@eecs.berkeley.edu
 *
 */
public class Cvc3BVBackend extends Cvc3Backend{

	public Cvc3BVBackend(SMTSketchOptions options, String tmpFilePath,
			RecursionControl rcontrol, TempVarGen varGen, boolean tracing)
			throws IOException {
		super(options, tmpFilePath, rcontrol, varGen, tracing);
	}

	@Override
    protected FormulaPrinter createFormulaPrinterInternal(NodeToSmtVtype formula,
            PrintStream ps)
    {
        return new Cvc3BVTranslator(formula, ps, mIntNumBits);
    }
	
	@Override
	public SmtValueOracle createValueOracle() {
		return new Cvc3BVOracle(mTrans);
	}
	
}
