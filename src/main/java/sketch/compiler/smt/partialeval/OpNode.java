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
		this.hashCode = computeHash();
	}
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		
		if (getOpcode() == OpCode.IF_THEN_ELSE) {
		    sb.append('(');
		    sb.append(getOperands()[0].toString());
		    sb.append(" ? ");
		    sb.append(getOperands()[1].toString());
		    sb.append(" : ");
		    sb.append(getOperands()[2].toString());
		    sb.append(')');
		} else {
		    sb.append('(');
	        for (NodeToSmtValue opnd : getOperands()) {
	            sb.append(getCanonicalOp(getOpcode()));
	            sb.append(opnd.toString());
	        }
	        sb.append(')');    
		}
	
		return sb.toString();
	}
	
	@Override
	public abstractValue clone() {
		return new OpNode(getType(), getNumBits(), getOpcode(), getOperands());
	}
	
	@Override
	public int hashCode() {
		return hashCode;
	}
	
	private int computeHash() {
	    int code = opcode.hashCode() ^ type.hashCode() ^ getOperands().length;
      for (NodeToSmtValue opnd : getOperands()) {
          code ^= opnd.hashCode();
      }
        return code;
	}
	
	@Override
	public boolean equals(Object obj) {
	    if (this == obj)
	        return true;

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
	
	public static String getCanonicalOp(OpCode opcode) {
	    if (opcode == OpCode.PLUS)
	        return "+";
	    else if (opcode == OpCode.MINUS)
	        return "-";
	    else if (opcode == OpCode.TIMES)
            return "*";
	    else if (opcode == OpCode.OVER)
            return "/";
	    else if (opcode == OpCode.AND)
            return "&&";
	    else if (opcode == OpCode.OR)
            return "||";
	    else if (opcode == OpCode.NOT)
            return "!";
	    else if (opcode == OpCode.XOR)
            return "^";
	    else if (opcode == OpCode.CONCAT)
            return "@";
	    
	    else if (opcode == OpCode.EQUALS)
            return "==";
	    else if (opcode == OpCode.LT)
            return "<";
	    else if (opcode == OpCode.LEQ)
            return "<=";
	    else if (opcode == OpCode.GT)
            return ">";
	    else if (opcode == OpCode.GEQ)
            return ">=";
	    else if (opcode == OpCode.LSHIFT)
            return "<<";
	    else if (opcode == OpCode.RSHIFT)
            return ">>";
	    
	    
	    else
	        return opcode.toString();
	}
	
	@Override
    public Object accept(FormulaVisitor fv) {
        return fv.visitOpNode(this);
    }
}