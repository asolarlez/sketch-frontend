package sketch.compiler.smt;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sketch.compiler.ast.core.Function;
import sketch.compiler.dataflow.abstractValue;
import sketch.compiler.smt.partialeval.LabelNode;
import sketch.compiler.smt.partialeval.LinearNode;
import sketch.compiler.smt.partialeval.NodeToSmtValue;
import sketch.compiler.smt.partialeval.OpNode;
import sketch.compiler.smt.partialeval.SmtType;
import sketch.compiler.smt.partialeval.VarNode;

/**
 * This is an abstract translator class for translating V 
 * and NodeToSmtState into SMT formulas. It's meant to be subclassed
 * for specific SMT formats
 * 
 * @author Lexin Shan
 * @email lshan@eecs.berkeley.edu
 *
 */
public abstract class SMTTranslator  {
	
	public enum OpCode {
		PLUS,
		MINUS,
		OVER,
		TIMES,
		MOD,
		
		EQUALS,
		NOT_EQUALS,
		LT,
		LEQ,
		GT,
		GEQ,
		// logical operators
		NOT,
		AND,
		NAND,
		OR,
		XOR,
		XNOR,
		// numerical operators
		NEG,
		NUM_NOT,
		NUM_AND,
		NUM_OR,
		NUM_EQ,
		NUM_XOR,
		// shifts
		LSHIFT,
		RSHIFT,
		// concat & extract
		CONCAT,
		EXTRACT,	// for bit array only
		REPEAT,
		// array
		ARRNEW,		
		ARRACC,		// only used by Theory-of-Array
		ARRUPD,		// only used by Theory-of-Array
		// short hands
		IF_THEN_ELSE,
		FUNCCALL,
	}
	
	protected Map<OpCode, String> opStrMap;

	
	public SMTTranslator() {
		opStrMap = new HashMap<OpCode, String>();
		initOpCode();
	}

	/*
	 * Public Functions
	 */

	public abstract String prolog();

	public abstract String getAssert(String predicate);
	
	/**
	 * Returns a string representation of the array access representation
	 * in the current SMTTranslator
	 * 
	 * @param dest the SSA num of the array after the update
	 * @param src the SSA num of the array before the update
	 * @param idx index of the element to be updated
	 * @param newVal new value
	 *
	 */
	protected abstract String getDefineVar(String type, String varName);
	
	public String getDefineVar(VarNode varNode) {
        String name = getStr(varNode);
        return getDefineVar(getTypeForSolver(varNode.getSmtType()), name);
    }
	
	public abstract String epilog();
	
	public abstract String getComment(String msg);
	
	public String getBlockComment(String[] msg) {
		StringBuffer sb = new StringBuffer();
		sb.append(getComment("\n"));
		
		for (String s : msg) {
			sb.append(getComment(s));
			sb.append('\n');
		}
		sb.append(getComment(""));
		return sb.toString();
	}
	
	/**
	 * Get String representation that does a N-ary operation on opnds
	 * This includes N-aru AND, OR, EXTRACT
	 * For bit vector operation EXTRACT, the first operand is the end
	 * of the range (inclusive), the second operand is the start of the 
	 * range (inclusive), the third operand is the value to extract from
	 * @param op
	 * @param opnds
	 * @return
	 */	
	public abstract String getNaryExpr(OpCode op, NodeToSmtValue... opnds);
	
	
	public abstract String getStrForLinearNode(LinearNode linNode);
	
	
	public abstract String getLetHead();
	public abstract String getLetLine(NodeToSmtValue dest, NodeToSmtValue def);
	public abstract String getLetFormula(NodeToSmtValue formula);
	public abstract String getLetTail(int numLets);
	
	
	
	/**
	 * Get String representation that does a unary operation on cond
	 * @param op
	 * @param opnd
	 * @return
	 */
	protected abstract String getUnaryExpr(OpCode op, String opnd);
	
	public String getUnaryExpr(OpCode op, NodeToSmtValue cond) {
		return getUnaryExpr(op, getStr(cond));
	}
	
	/**
	 * Get String representation for the given type
	 * @param t
	 * @return
	 */
	public abstract String getTypeForSolver(SmtType t);
	
	/**
	 * Get String representation for a function call
	 * @param fun
	 * @param avlist
	 * @param outSlist
	 */
	public String getFuncCall(Function fun, List<abstractValue> avlist, List<abstractValue> outSlist) {
		return getFuncCall(fun.getName(), avlist, outSlist);
	}
	
	public abstract String getFuncCall(String funName, List<abstractValue> avlist, List<abstractValue> outSlist);
	
	protected abstract void initOpCode();
	
	public String getOp(OpCode o) {
		assert opStrMap.containsKey(o) : "opStrMap is not fully initialized.";
		return opStrMap.get(o);
	}
		
	
	
	public String getStr(NodeToSmtValue ntsv) {
		
		if (ntsv.isConst()) {
			if (ntsv.isInt())
				return getStrAsInt(ntsv);
			else if (ntsv.isBool())
				return getStrAsBool(ntsv);
			else if (ntsv.isBit())
				return getStrAsBit(ntsv);
			else if (ntsv.isBitArray())
				return getStrAsBitArray(ntsv);
			else
				throw new IllegalStateException("there is an unknown constant type");
		
		} else {
			if (ntsv instanceof VarNode) {
				VarNode varNode = (VarNode) ntsv;
				String varName = varNode.getRHSName();
				if (varName.startsWith("_")) {
		            return "sk_" + varName;
		        }
				return varName;
			} else if (ntsv instanceof OpNode) {
				OpNode node = (OpNode) ntsv;
				return getNaryExpr(node.getOpcode(), node.getOperands());
			} else if (ntsv instanceof LabelNode) {
				return ntsv.toString();
			} else if (ntsv instanceof LinearNode) {
			    return getStrForLinearNode((LinearNode) ntsv);
			}
		
		}
		
		throw new IllegalStateException("getStr() called on unexpected NodeToSmtValue object");
	}
	
	public String stripVariableName(String varName) {
	    if (varName.startsWith("sk_"))
	        return varName.substring(3);
	    else
	        return varName;
	}
	
	/**
	 * precondition: val is a constant
	 * return a string representation of the boolean constant
	 * 
	 * @param val
	 * @return
	 */
	public String getStrAsBool(NodeToSmtValue val) {
		return (val.getIntVal() == 0) ? getFalseLiteral() : getTrueLiteral();
	}
	
	/**
	 * precondition: val is a constant
	 * return a string representation of the int constant
	 * @param val
	 * @return
	 */
	public String getStrAsInt(NodeToSmtValue val) {
		return getIntLiteral(val.getIntVal(), val.getSmtType().getNumBits());
	}
	
	/**
	 * precondition: val is a constant
	 * return a string representation of the bit constant
	 * @param val
	 * @return
	 */
	public String getStrAsBit(NodeToSmtValue val) {
		return getIntLiteral(val.getIntVal(), 1);
	}
	
	/**
	 * precondition: val is a constant bit array
	 * return a string representation of the bit array constant
	 * @param val
	 * @return
	 */
	public String getStrAsBitArray(NodeToSmtValue val) {
		return getBitArrayLiteral(val.getIntVal(), val.getSmtType().getNumBits());
	}
	
	public abstract String getTrueLiteral();
	public abstract String getFalseLiteral();
	public abstract String getIntLiteral(int i, int numBits);
	public abstract String getBitArrayLiteral(int i, int numBits);
	public abstract String getBitArrayLiteral(int numBits, int...arr);
	

}
