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
			handleBitArrayUpdate(vt, ntsvVal, ntsvIdx, vtype);
		} else {
			// 2) the state is any other kind of array
			handleNormalArrayUpdate(vt, ntsvVal, ntsvIdx, vtype);
		}
	}

	private void handleBitArrayUpdate(abstractValueType vt,
			NodeToSmtValue ntsvVal, NodeToSmtValue ntsvIdx,
			NodeToSmtVtype vtype) {
		
		int bitArrSize = BitVectUtil.vectSize(getType());
		NodeToSmtValue dest = state(vtype);
		NodeToSmtValue newVal = null;
		
		if (ntsvIdx.isConst()) {
			//    	if idx is constant, translate it as:
			//			x_i+1 = (concat (extract[bitArrSize-1:idx+1] x_i)
			//							(extract[idx:idx] x_i)
			//							(extract[idx-1:0] x_i))
			int iIdx =  ntsvIdx.getIntVal();
			
			newVal = ntsvVal;
			if (iIdx > 0)
				newVal = (NodeToSmtValue) vtype.concat(newVal, 
									vtype.extract(iIdx-1, 0, dest));
			
			if (iIdx < bitArrSize-1)
				newVal = (NodeToSmtValue) vtype.concat(	
						vtype.extract(bitArrSize-1, iIdx+1, dest), 
						newVal);
			
		} else {
			//		if idx is bottom:
			//			if idx == 0, x_i+1[0] = val else x_i+1[0] = x_i[0]
			//			if idx == 1, x_i+1[1] = val else x_i[1]
			//			... use a bunch of if-then-else to handle it
			// (= x_i+1 (ite (= idx bv0[32]) 
			//				(concat (extract[31:1] x_i) val)
			//				(ite (= idx bv1[32])
			//					(concat (concat (extract[31:2] x_i) val) (extract[0:0] x_i)))
			//					... continue nesting down
			// 
			int high = BitVectUtil.vectSize(this.getType()) - 1;
			
			newVal = getRecursiveIfThenElse(0, high, dest, ntsvIdx, ntsvVal);
		}
		
		super.update(newVal, vtype);
	}
	
	private NodeToSmtValue getRecursiveIfThenElse(int mid, int high, NodeToSmtValue dest, NodeToSmtValue ntsvIdx, NodeToSmtValue ntsvVal) {
		if (mid > high) {
			return dest;
		} else {
			
			NodeToSmtValue rhs;
			if (mid == 0) {
				// arr_0[high:mid+1] @ (if idx == mid) then ntsv else arr_0[mid:mid])
				NodeToSmtValue first = (NodeToSmtValue) vtype.extract(high, mid + 1, dest);
//				NodeToSmtValue second = vtype.condjoin(
//							vtype.eq(ntsvIdx, vtype.CONST(mid)),
//							ntsvVal,
//							vtype.extract(mid, mid, dest));
				NodeToSmtValue second = ntsvVal;
				
				rhs = (NodeToSmtValue) vtype.concat(first, second); 
				
			} else if (mid == high) {
				// ntsv @ arr_0[mid-1:0]
//				NodeToSmtValue first = vtype.condjoin( 
//							vtype.eq(ntsvIdx, vtype.CONST(mid)),
//							ntsvVal,
//							vtype.extract(mid, mid, dest));
				NodeToSmtValue first = ntsvVal;
				
				NodeToSmtValue second = (NodeToSmtValue) vtype.extract(mid - 1, 0, dest);
				rhs = (NodeToSmtValue) vtype.concat(first, second); 
			} else {
				// arr_0[high:mid+1] @ ntsv @ arr_0[mid-1:0]
				NodeToSmtValue first = (NodeToSmtValue) vtype.extract(high, mid + 1, dest);
//				NodeToSmtValue second = vtype.condjoin( 
//							vtype.eq(ntsvIdx, vtype.CONST(mid)),
//							ntsvVal,
//							vtype.extract(mid, mid, dest));
				NodeToSmtValue second = ntsvVal;
				
				NodeToSmtValue third = (NodeToSmtValue) vtype.extract(mid - 1, 0, dest);
				rhs = (NodeToSmtValue) vtype.concat(
						vtype.concat(first, second), 
						third);
				// concat three pieces together
			}
			
			NodeToSmtValue ite = vtype.condjoin( 
							vtype.eq(ntsvIdx, vtype.CONST(mid)),
							rhs,
							getRecursiveIfThenElse(mid + 1, high, dest, ntsvIdx, ntsvVal));
			return ite;
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
	
	protected void updateOneElement(int i, NodeToSmtValue newVal) {
		this.updateLHSidx(i);
		VarNode newDest = newLHSvalue(i);
		newDest.update(newVal);
		arrElems.put(i, newDest);

		vtype.declareRenamableVar(newDest);
		vtype.addDefinition(newDest, newVal);
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
