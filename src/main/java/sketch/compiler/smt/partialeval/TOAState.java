package sketch.compiler.smt.partialeval;

import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
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

    
    /**
     * Create a clean BlastArrayState object
     * @param name
     * @param t
     * @param vt
     */
    protected TOAState(String name, SmtType t, NodeToSmtVtype vt) {
        this(name, t, vt, null);
        
        if (BitVectUtil.isNormalArray(t.getRealType()))
            vtype.declareArraySeedVariable((VarNode) this.absVal);
    }
    
	protected TOAState(String name, SmtType t, NodeToSmtVtype vt, TOAState parent) {
		super(name, t, vt);
		
		
		if (parent != null)
            helperDeltaClone(parent, vt);
		
		lhsIdxs = idxsArr(1);

        if (parent != null)
            init(parent.absVal);
        else
            init(newLHSvalue());

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
		
		if (BitVectUtil.isBitArray(this.getType())) {
            // two cases:
            // 1) the state is bit array, in which case, handle it as if
            //      theory of array - increment the ssa number.
            //      Example:
            //          x_i[idx] = val;
            handleBitArrayUpdate(ntsvVal, ntsvIdx, vtype);
		} else {
		
		    TypeArray ta = (TypeArray) destVal.getType();
		    ntsvVal = makeRhsConformLhsWidth(vtype.getNumBitsForType(ta.getBase()), ntsvVal, vtype);
		    
    		this.updateLHSidx();
    		this.absVal = newLHSvalue();
    		VarNode newDest = (VarNode) state(vt);
    		vtype.addDefinition(newDest,
    		        (NodeToSmtValue) vtype.arrupd(destVal, ntsvIdx, ntsvVal));
		}
	}

	public varState getDeltaClone(abstractValueType vt) {
		TOAVtype ntsvt = ((TOAVtype) vt);
		TOAState st = new TOAState(name, this.getSmtType(), ntsvt, this);
		
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
