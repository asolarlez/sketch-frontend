package sketch.compiler.smt.partialeval;

import java.util.LinkedList;
import java.util.List;

import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.dataflow.abstractValue;
import sketch.compiler.smt.SMTTranslator.OpCode;

public class OpNode extends NodeToSmtValue {
	OpCode opcode;
	
	public OpCode getOpcode() {
		return opcode;
	}
	
	public NodeToSmtValue[] getOperands() {
		return (NodeToSmtValue[]) obj;
	}
	
	@Override
	public List<abstractValue> getVectValue() {
		List<abstractValue> l = new LinkedList<abstractValue>();
		for (NodeToSmtValue elem : getOperands()) {
			l.add(elem);
		}
		return l;
	}
	
	public OpNode(Type realType, int numBits, OpCode opcode, NodeToSmtValue...operands) {
		super(null, realType, SmtStatus.BOTTOM, numBits, null);
		this.opcode = opcode;
		this.obj = operands;
	}
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		
		sb.append(getOpcode());
		sb.append('(');
		for (NodeToSmtValue opnd : getOperands()) {
			sb.append(opnd.toString());
			sb.append(',');
		}
		sb.append(')');
		return sb.toString();
	}
	
	@Override
	public abstractValue clone() {
		return new OpNode(getType(), getNumBits(), getOpcode(), getOperands());
	}
	
	@Override
	public int hashCode() {
		int code = opcode.hashCode() ^ type.hashCode() ^ getOperands().length;
//		for (NodeToSmtValue opnd : getOperands()) {
//			code ^= opnd.hashCode();
//		}
		return code;
	}
	
	@Override
	public boolean equals(Object obj) {

		if (obj instanceof OpNode) {
			OpNode that = (OpNode) obj;
			
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
}