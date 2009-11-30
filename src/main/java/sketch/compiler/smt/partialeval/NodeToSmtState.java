package sketch.compiler.smt.partialeval;

import java.util.List;

import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.dataflow.abstractValue;
import sketch.compiler.dataflow.abstractValueType;


public abstract class NodeToSmtState extends TypedState {

	public class lhsIndexes {
		public int idx = 1;
	}

	protected NodeToSmtVtype vtype;

	protected lhsIndexes[] lhsIdxs;

	
	/**
	 * Constructor
	 * @param t
	 */
	public NodeToSmtState(String name, Type t, TypedVtype vt) {
		super(name, t, vt);
		this.vtype = (NodeToSmtVtype) vt;
	}

	/**
	 * creates a new LHS value 
	 */
	public NodeToSmtValue newLHSvalue() {
		return vtype.STATE_DEFAULT(name,
				getType(), 
				this.vtype.getNumBitsForType(getType()),
				((NodeToSmtState) this.rootParent()).lhsIdxs[0].idx);
	}

	
	public SmtType getSmtType() {
		return (SmtType) super.getType();
	}
	
	@Override
	public Type getType() {
		return getSmtType().getRealType();
	}

	protected lhsIndexes[] idxsArr(int sz) {
		lhsIndexes[] rv = new lhsIndexes[sz];
		for (int i = 0; i < sz; ++i) {
			rv[i] = new lhsIndexes();
		}
		return rv;
	}
	
	protected void updateLHSidx() {
		((NodeToSmtState) this.rootParent()).lhsIdxs[0].idx++;
	}
	
	protected void updateLHSidx(int iidx) {
		((NodeToSmtState) this.rootParent()).lhsIdxs[iidx].idx++;
	}

	public NodeToSmtValue state(int i) {
		return (NodeToSmtValue) super.state(i);
	}
	
	public NodeToSmtValue state(abstractValueType vtype) {
		return (NodeToSmtValue) super.state(vtype);
	}
	
	/**
	 * Update for a single variable
	 */
	public void update(abstractValue val, abstractValueType vt) {
		NodeToSmtValue ntsvVal = (NodeToSmtValue) val;
		NodeToSmtValue oldDest = state(vt);
		// make sure the RHS's width conforms with LHS
		if (oldDest.getNumBits() < ntsvVal.getNumBits()) {
			ntsvVal = vtype.extract(oldDest.getNumBits()-1, 0, ntsvVal);
		} else if (oldDest.getNumBits() > ntsvVal.getNumBits()) {
			ntsvVal = vtype.padIfNotWideEnough(ntsvVal, oldDest.getNumBits());
		}
		
		if (!this.isArr()) {
			// normal assignment
			
			// if ntsvVal is OpNode, try structure hashing, see if
			// there is another variable we can use
			if (ntsvVal instanceof OpNode) {
				this.updateLHSidx();
				this.absVal = newLHSvalue();
				VarNode newDest = (VarNode) state(vt);
				vtype.declareLocalVar(newDest);
				vtype.addDefinition(newDest, ntsvVal);
				
			} else if (ntsvVal instanceof VarNode ||
					ntsvVal instanceof ConstNode ||
					ntsvVal instanceof LinearNode) {
				// if is VarNode or ConstNode, use it as absVal
				this.absVal = ntsvVal;
			} else {
			    throw new IllegalStateException("updating NodeToSmtState with an unexpected value");
			}
			
		
		} else {
			// NO-OP. if the state is an array, it must be the case that we are
			// assigning an array to another array, which means we need
			// to do element-wise copy, that is handled in the super.update();
			
			if (arrElems == null) {
				init(val.getVectValue().size());
			}
			
			int lv = this.numKeys();
			List<abstractValue> vlist = val.getVectValue();
			for (int i = 0; i < lv; ++i) {
				abstractValue cv;
				if (i < vlist.size()) {
					cv = vlist.get(i);
				} else {
					cv = vtype.CONST(0);
				}
				update(vtype.CONST(i), cv, vtype);
			}
		}
	}
	
	protected void handleBitArrayUpdate(
            NodeToSmtValue ntsvVal, NodeToSmtValue ntsvIdx,
            NodeToSmtVtype vtype) {
        
        int bitArrSize = BitVectUtil.vectSize(getType());
        NodeToSmtValue dest = state(vtype);
        NodeToSmtValue newVal = null;
        
        if (ntsvIdx.isConst()) {
            //      if idx is constant, translate it as:
            //          x_i+1 = (concat (extract[bitArrSize-1:idx+1] x_i)
            //                          (extract[idx:idx] x_i)
            //                          (extract[idx-1:0] x_i))
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
            //      if idx is bottom:
            //          if idx == 0, x_i+1[0] = val else x_i+1[0] = x_i[0]
            //          if idx == 1, x_i+1[1] = val else x_i[1]
            //          ... use a bunch of if-then-else to handle it
            // (= x_i+1 (ite (= idx bv0[32]) 
            //              (concat (extract[31:1] x_i) val)
            //              (ite (= idx bv1[32])
            //                  (concat (concat (extract[31:2] x_i) val) (extract[0:0] x_i)))
            //                  ... continue nesting down
            // 
            int high = BitVectUtil.vectSize(this.getType()) - 1;
            
            newVal = getRecursiveIfThenElse(0, high, dest, ntsvIdx, ntsvVal);
        }
        
        this.update(newVal, vtype);
    }
    
    private NodeToSmtValue getRecursiveIfThenElse(int mid, int high, NodeToSmtValue dest, NodeToSmtValue ntsvIdx, NodeToSmtValue ntsvVal) {
        if (mid > high) {
            return dest;
        } else {
            
            NodeToSmtValue rhs;
            if (mid == 0) {
                // arr_0[high:mid+1] @ (if idx == mid) then ntsv else arr_0[mid:mid])
                NodeToSmtValue first = (NodeToSmtValue) vtype.extract(high, mid + 1, dest);
//              NodeToSmtValue second = vtype.condjoin(
//                          vtype.eq(ntsvIdx, vtype.CONST(mid)),
//                          ntsvVal,
//                          vtype.extract(mid, mid, dest));
                NodeToSmtValue second = ntsvVal;
                
                rhs = (NodeToSmtValue) vtype.concat(first, second); 
                
            } else if (mid == high) {
                // ntsv @ arr_0[mid-1:0]
//              NodeToSmtValue first = vtype.condjoin( 
//                          vtype.eq(ntsvIdx, vtype.CONST(mid)),
//                          ntsvVal,
//                          vtype.extract(mid, mid, dest));
                NodeToSmtValue first = ntsvVal;
                
                NodeToSmtValue second = (NodeToSmtValue) vtype.extract(mid - 1, 0, dest);
                rhs = (NodeToSmtValue) vtype.concat(first, second); 
            } else {
                // arr_0[high:mid+1] @ ntsv @ arr_0[mid-1:0]
                NodeToSmtValue first = (NodeToSmtValue) vtype.extract(high, mid + 1, dest);
//              NodeToSmtValue second = vtype.condjoin( 
//                          vtype.eq(ntsvIdx, vtype.CONST(mid)),
//                          ntsvVal,
//                          vtype.extract(mid, mid, dest));
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

	protected void callParentUpdate(abstractValue val, abstractValueType vt) {
		super.update(val, vt);
	}
	
	/**
     * Make RHS conform with LHS width
     * @param lhsVal
     * @param rhsVal
     * @param vtype
     * @return
     */
    protected NodeToSmtValue makeRhsConformLhsWidth(int lhsNumBits,
            final NodeToSmtValue rhsVal, NodeToSmtVtype vtype)
    {
        NodeToSmtValue newVal = rhsVal;
        // make sure the RHS's width conforms with LHS
        if (lhsNumBits < rhsVal.getNumBits()) {
            newVal = vtype.extract(lhsNumBits-1, 0, rhsVal);
        } else if (lhsNumBits > rhsVal.getNumBits()) {
            newVal = vtype.padIfNotWideEnough(rhsVal, lhsNumBits);
        }
        return newVal;
    }

}
