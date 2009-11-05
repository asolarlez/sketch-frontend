package sketch.compiler.smt.cvc3;

import java.io.IOException;

import sketch.compiler.CommandLineParamManager;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.dataflow.recursionCtrl.RecursionControl;
import sketch.compiler.smt.SMTTranslator;
import sketch.compiler.smt.partialeval.SmtValueOracle;

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
	protected SMTTranslator createSMTTranslator() {
		return new Cvc3BVTranslator(params.flagValue("inbits"));
	}
	
	@Override
	protected SmtValueOracle createValueOracle() {
		return new Cvc3BVOracle();
	}
	
}
