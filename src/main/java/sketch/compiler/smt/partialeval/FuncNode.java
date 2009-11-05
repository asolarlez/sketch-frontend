package sketch.compiler.smt.partialeval;

import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.smt.SMTTranslator.OpCode;

public class FuncNode extends OpNode {
	
	public FuncNode(Type realRetType, int numBits, String funcName,
			NodeToSmtValue...args) {
		super(realRetType, numBits, OpCode.FUNCCALL, args);
		name = funcName;
	}
	
	@Override
	public int hashCode() {
		return name.hashCode() ^ super.hashCode();
	}
	
	@Override
	public boolean equals(Object arg0) {
		if (this == obj) return true;
		
		if (arg0 instanceof FuncNode) {
			FuncNode that = (FuncNode) arg0;
			
			if (that.getName().equals(this))
				return false;
			
			NodeToSmtValue[] thisOpnds = this.getOperands();
			NodeToSmtValue[] thatOpnds = that.getOperands();
			if (thisOpnds.length != thatOpnds.length)
				return false;
			
			int i = 0;
			for (NodeToSmtValue thisOpnd :thisOpnds) {
				if (thatOpnds[i] != (thisOpnd))
					return false;
				i++;
			}
			
			return this.type.equals(that.type);
		}
		return false;
	}
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		
		sb.append(name);
		sb.append('(');
		for (NodeToSmtValue opnd : getOperands()) {
			sb.append(opnd.toString());
			sb.append(',');
		}
		sb.append(')');
		return sb.toString();
	}
	
}