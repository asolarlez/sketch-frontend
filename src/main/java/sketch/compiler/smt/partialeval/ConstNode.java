package sketch.compiler.smt.partialeval;

import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.dataflow.abstractValue;

public class ConstNode extends NodeToSmtValue {
	
	public ConstNode(Type t, int size, int value) {
		super(null, t, SmtStatus.CONST, size, null);
		this.obj = value;
	}
	
	@Override
	public String toString() {
		if (type == TypePrimitive.booltype) {
			return ((Integer) obj) == 0 ? "FALSE" : "TRUE";
		} else {
			return obj + "";
		}
	}
	
	@Override
	public abstractValue clone() {
		return new ConstNode(getType(), getNumBits(), (Integer) this.obj);
	}
	
	@Override
	public int hashCode() {
		return type.hashCode() ^ obj.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		
		if (obj instanceof ConstNode) {
			ConstNode that = (ConstNode) obj;
			return this.obj.equals(that.obj) &&
				this.type.equals(that.type);
				
		}
		return false;
	}
}