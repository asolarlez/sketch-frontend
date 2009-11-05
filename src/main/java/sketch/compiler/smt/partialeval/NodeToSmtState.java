package sketch.compiler.smt.partialeval;

import java.util.List;

import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.dataflow.abstractValue;
import sketch.compiler.dataflow.abstractValueType;

public abstract class NodeToSmtState extends TypedState {

	public class lhsIndexes {
		public int idx = 1;
	}

	NodeToSmtVtype vtype;

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
				vtype.declareRenamableVar(newDest);
				vtype.addDefinition(newDest, ntsvVal);
				
			} else if (ntsvVal instanceof VarNode ||
					ntsvVal instanceof ConstNode) {
				// if is VarNode or ConstNode, use it as absVal
				this.absVal = ntsvVal;
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

	protected void callParentUpdate(abstractValue val, abstractValueType vt) {
		super.update(val, vt);
	}

}
