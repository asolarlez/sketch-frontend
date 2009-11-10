package sketch.compiler.smt.partialeval;

import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.dataflow.abstractValue;

public  class VarNode extends NodeToSmtValue {
	/**
	 * This value is shared with NodeToSmtState
	 * This value can be null for a BOTTOM value because it can 
	 * never increment the SSA number
	 */
	private int rhsIdx = 0;

	public int getrhsIdx() {
		return rhsIdx;
	}
	
	public VarNode(String name, Type t, int numBits, int rhsIdx) {
		super(name, t, SmtStatus.BOTTOM, numBits, null);
		this.rhsIdx = rhsIdx;		
	}
	
	
	public void update(abstractValue v) {
//		assert false : "NodeToSmtValue.update() is deprecated";
		NodeToSmtValue ntsv = (NodeToSmtValue) v;
		assert !isVolatile : "NodeToSmtValue does not support volatility.";
		
		this.smtStatus = ntsv.smtStatus;
		this.obj = ntsv.obj;
	}
	
	public String getRHSName() {
		// if lhsIdx is null, that means this NodeToSmtValue
		// is required to use a certain name
		
		if (this.suffixSetter != null && rhsIdx < 0) {
			return getName() + this.suffixSetter.getSuffix();
			
		} else if (this.suffixSetter != null && rhsIdx >= 0){
			return getName() + "_" + this.getrhsIdx() + this.suffixSetter.getSuffix();
			
		} else if (this.suffixSetter == null && rhsIdx < 0) {
			// these are the fixed name variables
			return getName();
			
		} else {
			return getName() + "_" + this.getrhsIdx();
		}
	}
	
	@Override
	public String toString() {
		return getRHSName();
	}
	
	@Override
	public abstractValue clone() {
		return new VarNode(this.name, getType(), getNumBits(), rhsIdx);
	}
	
	@Override
	public int hashCode() {
		return rhsIdx ^ name.hashCode() ^ type.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj instanceof VarNode) {
			VarNode that = (VarNode) obj;
			return this.name.equals(that.name) &&
				this.type.equals(that.type) &&
				this.rhsIdx == that.rhsIdx;
		}
		return false;
	}
	
	@Override
    public void accept(FormulaVisitor fv) {
        fv.visitVarNode(this);
    }
}