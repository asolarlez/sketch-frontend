package sketch.compiler.smt.stp;

import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.dataflow.abstractValue;
import sketch.compiler.smt.SMTTranslator;
import sketch.compiler.smt.SMTTranslator.OpCode;
import sketch.compiler.smt.partialeval.BlastArrayVtype;
import sketch.compiler.smt.partialeval.NodeToSmtValue;

public class STPVtype extends BlastArrayVtype {

	public STPVtype(SMTTranslator smtTran, 
			int intNumBits,
			int inBits,
			int cBits,
			TempVarGen tmpVarGen) {
		super(smtTran, intNumBits, inBits, cBits, tmpVarGen);

	}
	
	@Override
	public NodeToSmtValue eq(abstractValue v1, abstractValue v2) {
		NodeToSmtValue left = (NodeToSmtValue) v1;
		NodeToSmtValue right = (NodeToSmtValue) v2;
		
		if (left.isBool() && right.isBool()) {
			return not(xor(left, right));
		}
		return super.eq(v1, v2);
	}
	
	@Override
	public NodeToSmtValue shr(abstractValue v1, abstractValue v2) {
		NodeToSmtValue left = (NodeToSmtValue) v1;
		NodeToSmtValue right = (NodeToSmtValue) v2;
		if (right.isConst()) {
			if (right.getIntVal() > 0)
				return BOTTOM(left.getType(), 
						OpCode.RSHIFT, left, right);
			else
				return left;
		} else
			return shiftBottomHelper(0, 32, false, v1, v2);
	}
	
	@Override
	public NodeToSmtValue shl(abstractValue v1, abstractValue v2) {
		NodeToSmtValue left = (NodeToSmtValue) v1;
		NodeToSmtValue right = (NodeToSmtValue) v2;
		if (right.isConst()) {
			if (right.getIntVal() > 0)
				
				return 
					extract(left.getNumBits() -1, 0, 
						BOTTOM(left.getType(),
								OpCode.LSHIFT, left, right)
								);
			else
				return left;
		} else
			return shiftBottomHelper(0, 32, true, v1, v2);
			
	}
	
	NodeToSmtValue shiftBottomHelper(int idx, int lastIdx, boolean isLeftShift, abstractValue v1, abstractValue v2) {
		if (idx == 0) {
			return (NodeToSmtValue) condjoin(
					eq(v2, CONST(idx)), 
					v1, 
					shiftBottomHelper(idx+1, lastIdx, isLeftShift, v1, v2));
		}
		else if (idx == lastIdx )
			return isLeftShift ? shl(v1, CONST(idx)) : shr(v1, CONST(idx));
		else
			return (NodeToSmtValue) condjoin(
					eq(v2, CONST(idx)), 
					isLeftShift ? shl(v1, CONST(idx)) : shr(v1, CONST(idx)), 
					shiftBottomHelper(idx+1, lastIdx, isLeftShift, v1, v2));
	}

}
