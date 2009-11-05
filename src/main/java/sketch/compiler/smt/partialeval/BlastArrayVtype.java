package sketch.compiler.smt.partialeval;

import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.dataflow.MethodState;
import sketch.compiler.dataflow.varState;
import sketch.compiler.smt.SMTTranslator;

/**
 * This class is for translating to SMT formulas that blast array elements into 
 * individual variables
 * 
 * @author Lexin Shan
 * @email lshan@eecs.berkeley.edu
 *
 */
public class BlastArrayVtype extends NodeToSmtVtype {

	public BlastArrayVtype(
			SMTTranslator smtTran,
			int intNumBits,
			int inBits,
			int cBits,
			TempVarGen varGen) {
		super(smtTran, intNumBits, inBits, cBits, varGen);
		
	}


	@Override
	public varState cleanState(String var, Type t, MethodState mstate) {
		SmtType type = SmtType.create(t, getNumBitsForType(t));
		String properName = mTrans.getProperVarName(var);
		NodeToSmtState ret = new BlastArrayState(properName, type, this);
		return ret;
	}

	/**
	 * Partial eval the array-access node when the index expression is BOTTOM
	 */
	@Override
	protected NodeToSmtValue rawArracc(TypedValue arr, TypedValue idx, TypedValue len) {
		NodeToSmtValue ntsvArr = (NodeToSmtValue) arr;
		NodeToSmtValue ntsvIdx = (NodeToSmtValue) idx;
		// (ite (= idx 1) a_0
		//		(ite (= idx 2) a_1) ...)
		String newTmpVarName = tmpVarGen.nextVar("rawArracTmp");
		Type eleType = TypedVtype.getElementType(ntsvArr.getType());
		if (len.getIntVal() != 1)
			eleType = new TypeArray(eleType, new ExprConstInt(len.getIntVal()));
		varState tmpVarState = cleanState(newTmpVarName, eleType, getMethodState());
		int arrSize = arr.isBitArray() ? BitVectUtil.vectSize(ntsvArr.getType()) : arr.getVectValue().size();
		NodeToSmtValue newVal = rawArraccRecursive(ntsvArr, ntsvIdx, 0, arrSize, len.getIntVal());
		tmpVarState.update(newVal, this);
		
	
		return (NodeToSmtValue) tmpVarState.state(this);
	}
	
	private NodeToSmtValue rawArraccRecursive(NodeToSmtValue arr, NodeToSmtValue idx, int i, int size, int len) {
		
		if (i + len - 1 == size - 1) {
			// the last element
			if (arr.isBitArray()) {
				return extract(i+len-1, i, arr);
			} else {
				return (NodeToSmtValue) arr.getVectValue().get(i);
			}
		} else {
			if (arr.isBitArray()) {
				return condjoin(eq(idx, CONST(i)), extract(i+len-1, i, arr), 
						rawArraccRecursive(arr, idx, i+1, size, len));
			} else {
				return condjoin(eq(idx, CONST(i)), (NodeToSmtValue) arr.getVectValue().get(i), 
					rawArraccRecursive(arr, idx, i+1, size, len));
			}
		}
	}

}
