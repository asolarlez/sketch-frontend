package sketch.compiler.smt.partialeval;

import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.dataflow.MethodState;
import sketch.compiler.dataflow.varState;
import sketch.compiler.smt.SMTTranslator;
import sketch.compiler.solvers.constructs.AbstractValueOracle;

/**
 * Emit formula to use Theory Of Array
 * 
 * @author Lexin Shan
 * @email lshan@eecs.berkeley.edu
 *
 */
public class TOAVtype extends NodeToSmtVtype {
	
	public TOAVtype(AbstractValueOracle oracle, 
			SMTTranslator smtTran, 
			int intNumBits,
            int inBits,
            int cBits,
			TempVarGen varGen) {
		super(smtTran, intNumBits, inBits, cBits, varGen);
	}	

	@Override
	public varState cleanState(String var, Type t, MethodState mstate) {
		String properName = mTrans.getProperVarName(var);

		NodeToSmtState ret = new TOAState(properName, t, this);
		return ret;
	}
}
