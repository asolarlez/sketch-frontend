package sketch.compiler.smt.partialeval;

import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypeStructRef;
import sketch.compiler.dataflow.abstractValue;
import sketch.compiler.dataflow.abstractValueType;
import sketch.compiler.dataflow.varState;

/**
 * 
 * a[x] = x; // track individual element, see update()
 * 
 * @author Lexin Shan
 * @email lshan@eecs.berkeley.edu
 * 
 */
public class BlastArrayState extends NodeToSmtState {


	/**
	 * Create a clean BlastArrayState object
	 * @param name
	 * @param t
	 * @param vt
	 */
	protected BlastArrayState(String name, SmtType t, NodeToSmtVtype vt) {
		this(name, t, vt, null);
	}
	
	/**
	 * Create a BlastArrayState object that is a delta clone of another one
	 * A clone state object should have the following properties
	 * 
	 * parent field points to another state object that is its parent
	 * absVal field points to the absVal value of its parent. the absVal is 
	 * 			for later reference by the subsequent code in this if-branch
	 * @param name
	 * @param t
	 * @param vt
	 * @param parent
	 */
	protected BlastArrayState(String name, SmtType t, NodeToSmtVtype vt, BlastArrayState parent) {
		super(name, t, vt);

		if (parent != null)
			helperDeltaClone(parent, vt);

		if (BitVectUtil.isPrimitive(t.getRealType()) || t.getRealType() instanceof TypeStructRef) {
			lhsIdxs = idxsArr(1);

			if (parent != null)
				init(parent.absVal);
			else
				init(newLHSvalue());

		} else if (t.getRealType() instanceof TypeArray) {
			TypeArray tarr = (TypeArray) t.getRealType();
			abstractValue av = typeSize(tarr, vtype);
			if (av.hasIntVal()) {
				int arrsz = av.getIntVal();
				lhsIdxs = idxsArr(arrsz);
				// create an array of values for the state
				init(arrsz);
			} else {
				init(-1);
				lhsIdxs = null;
			}
		} else {
			throw new IllegalStateException("Unhandled case");
		}
	}

	public varState getDeltaClone(abstractValueType vt) {
		BlastArrayVtype ntsvt = ((BlastArrayVtype) vt);
		BlastArrayState st = new BlastArrayState(name, this.getSmtType(), ntsvt, this);
		
		if (st.rootParent() != this)
			st.lhsIdxs = null;
		return st;
	}

	@Override
	public void update(abstractValue idx, abstractValue val,
			abstractValueType vt) {
		NodeToSmtValue ntsvVal = (NodeToSmtValue) val;
		NodeToSmtValue ntsvIdx = (NodeToSmtValue) idx;
		NodeToSmtVtype vtype = (NodeToSmtVtype) vt;
		
		if (BitVectUtil.isBitArray(this.getType())) {
			// two cases:
			// 1) the state is bit array, in which case, handle it as if
			//	  	theory of array - increment the ssa number.
			//		Example:
			//  		x_i[idx] = val;
		    handleBitArrayUpdate(ntsvVal, ntsvIdx, vtype);
		} else {
			// 2) the state is any other kind of array
			handleNormalArrayUpdate(vt, ntsvVal, ntsvIdx, vtype);
		}
	}

	

	private void handleNormalArrayUpdate(abstractValueType vt, 
			final NodeToSmtValue ntsvVal,
			NodeToSmtValue ntsvIdx, 
			NodeToSmtVtype vtype) {
		NodeToSmtValue newVal = (NodeToSmtValue) ntsvVal;
		
		if (ntsvIdx.hasIntVal()) {
	
			int iidx = ntsvIdx.getIntVal();
			NodeToSmtValue dest = state(iidx);
			
			// make sure the RHS's width conforms with LHS
			if (dest.getNumBits() < ntsvVal.getNumBits()) {
				newVal = vtype.extract(dest.getNumBits()-1, 0, ntsvVal);
			} else if (dest.getNumBits() > ntsvVal.getNumBits()) {
				newVal = vtype.padIfNotWideEnough(ntsvVal, dest.getNumBits());
			}
			
			updateOneElement(iidx, newVal);
			
		} else {
			// translate to
			// (NOT (idx == i) OR state = val) AND
			// (NOT (idx == i) OR state = val) AND
			// ...
			
			// (= state_0_j+1 (ite (= idx 0) val state_0_j) 
			// (= state_1_j+1 (ite (= idx 1) val state_1_j) 
		
			for (int i = 0; i < this.numKeys(); i++) {
				NodeToSmtValue dest = state(i);
				
				// make sure the RHS's width conforms with LHS
				if (dest.getNumBits() < ntsvVal.getNumBits()) {
					newVal = vtype.extract(dest.getNumBits()-1, 0, ntsvVal);
				} else if (dest.getNumBits() > ntsvVal.getNumBits()) {
					newVal = vtype.padIfNotWideEnough(ntsvVal, dest.getNumBits());
				}
				NodeToSmtValue finalVal = vtype.condjoin(
						vtype.eq(ntsvIdx, vtype.CONST(i)), 
						newVal, 
						dest);
				
				updateOneElement(i, finalVal);
			}	
		}
	}
	
	protected void updateOneElement(int i, NodeToSmtValue ntsvVal) {
		
		
		if (ntsvVal instanceof OpNode) {
		    this.updateLHSidx(i);
            VarNode newDest = newLHSvalue(i);
            newDest.update(ntsvVal);
            arrElems.put(i, newDest);
            vtype.declareRenamableVar(newDest);
            vtype.addDefinition(newDest, ntsvVal);
            
        } else if (ntsvVal instanceof VarNode ||
                ntsvVal instanceof ConstNode ||
                ntsvVal instanceof LinearNode) {
            // if is VarNode or ConstNode, use it as absVal
            arrElems.put(i, ntsvVal);
        } else {
            throw new IllegalStateException("updating NodeToSmtState with an unexpected value");
        }
	}
	
	/**
	 * 
	 * Always create a new NodeToSmtValue object for the LHS
	 * 
	 * @param idx
	 * @param val
	 * @param vtype
	 */
	void updateToBottom(NodeToSmtValue idx, NodeToSmtValue val, NodeToSmtVtype vtype) {

		assert arrElems != null;
		if (arrElems == null) {
			if (absVal.isVect()) {
				assert false : "NYI";
			} else {
				absVal.update(vtype.BOTTOM());
			}
		}
		if (idx.hasIntVal()) {
			int iidx = idx.getIntVal();
			
			abstractValue newVal = newLHSvalue(iidx);
			if (parent != null) {
				abstractValue tmp = state(iidx);
				if (tmp.isVolatile()) {
					newVal.makeVolatile();
				}
			}
			arrElems.put(iidx, newVal);
			newVal.update(val);
		
		} else {
			abstractValue bottom = vtype.BOTTOM(val.getType());
			int lv = this.numKeys();
			for (int i = 0; i < lv; ++i) {
				abstractValue newVal = newLHSvalue(i);
				if (parent != null) {
					abstractValue tmp = state(i);
					if (tmp.isVolatile()) {
						newVal.makeVolatile();
					}
				}
				arrElems.put(i, newVal);
				newVal.update(bottom);

			}
		}
	}

	/**
	 * creates a new LHS value for idx i
	 * give it an appropriate name
	 */
	public VarNode newLHSvalue(int i) {
		Type stateType = getType();
		Type valueType = null;
		if (stateType instanceof TypeArray) {
			// This is called to create the NodeToSmtValue object for elements in the array
			valueType = ((TypeArray) stateType).getBase();
		} else {
			valueType = stateType;
		}
		
		return vtype.STATE_ELE_DEFAULT(name + "_idx_" + i,
				valueType, 
				this.vtype.getNumBitsForType(valueType),
				((NodeToSmtState) this.rootParent()).lhsIdxs[i].idx);
	}

}
