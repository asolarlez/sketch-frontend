package sketch.compiler.smt.partialeval;

import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.ast.core.typs.TypeStructRef;
import sketch.compiler.dataflow.abstractValue;
import sketch.compiler.dataflow.abstractValueType;
import sketch.compiler.dataflow.varState;

/**
 * Emit formula to use Theory Of Array
 * 
 * @author Lexin Shan
 * @email lshan@eecs.berkeley.edu
 *
 */
public class TOAState extends NodeToSmtState {

	protected TOAState(String name, Type t, NodeToSmtVtype vtype) {
		super(name, t, vtype);
		
		if (t instanceof TypePrimitive) {
			lhsIdxs = idxsArr(1);
			init(newLHSvalue());
		} else if (t instanceof TypeStructRef) {
			lhsIdxs = idxsArr(1);
			init(newLHSvalue());
		} else if (t instanceof TypeArray) {
			TypeArray tarr = (TypeArray) t;

			// Theory of array
			// give a value to the array itself
			lhsIdxs = idxsArr(1);
			init(newLHSvalue());

		} else {
			assert false : "This is an error.";
		}

	}

	/**
	 * Update the state of the form x_i[idx] = val;
	 * 
	 * The state variable should be the array x_i+1
	 * 
	 */
	public void update(abstractValue idx, abstractValue val,
			abstractValueType vt) {
		NodeToSmtValue ntsvVal = (NodeToSmtValue) val;
		NodeToSmtValue destVal = state(vtype);
		NodeToSmtValue ntsvIdx = (NodeToSmtValue) idx;
		
		// FIXME this is certainly wrong. fix this.
//		String destName = vtype.mTrans.getDefStr(destVal);
//		vtype.declareVar(this.getSmtType(),
//				destName);
//
//		
//		// / normal assignment
//		vtype.addAssert(vtype.mTrans.getAssignment(destVal, ntsvIdx,
//				ntsvVal));
//		
//		
//		this.callParentUpdate(vtype.BOTTOM(destName, destVal.getSmtType()), vt);
	}

	public varState getDeltaClone(abstractValueType vt) {
		TOAVtype ntsvt = ((TOAVtype) vt);
		TOAState st = new TOAState(name, this.t, ntsvt);
		st.helperDeltaClone(this, vtype);
		if (st.rootParent() != this)
			st.lhsIdxs = null;
		return st;
	}

	@Override
	public NodeToSmtValue newLHSvalue(int i) {
		Type stateType = getType();
		Type valueType = null;
		if (stateType instanceof TypeArray) {
			// This is called to create the NodeToSmtValue object for elements in the array
			valueType = ((TypeArray) stateType).getBase();
		} else {
			valueType = stateType;
		}
		// just create one single value for the array
		// FIXME passing null is wrong
		return null;
	}

}
