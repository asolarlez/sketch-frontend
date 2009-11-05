package sketch.compiler.smt.partialeval;

import sketch.compiler.ast.core.typs.Type;

public class LabelNode extends NodeToSmtValue {
	
	public LabelNode(Type t, int size, String rep) {
		super(null, t, SmtStatus.CONST, size, rep);
	}
	
	@Override
	public boolean isConst() {
		return false;
	}
	
	@Override
	public boolean isLabel() {
		return true;
	}
	
	@Override
	public String toString() {
		return (String) obj;
	}
	
	@Override
	public int hashCode() {
		return obj.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof OpNode) {
			VarNode that = (VarNode) obj;
			return this.obj.equals(that.obj) &&
				this.type.equals(that.type);
				
		}
		return false;
	}
}