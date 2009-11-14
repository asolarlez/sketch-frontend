package sketch.compiler.smt.partialeval;

import java.util.List;

import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.dataflow.abstractValue;
import sketch.compiler.smt.SMTTranslator.OpCode;

/**
 * 
 * CONST:
 * 		1) value representable by int32.		newInt, newBool, etc
 * 			label == null
 * 			obj == int
 * 			status == CONST
 * 			renamable == N/A
 * 			declarable == NO
 * 
 * 
 * 
 * BOTTOM:
 * 		1) value NOT representable by int32		newBottom
 * 			label == the string representation of the value
 * 			obj == label
 * 			status == BOTTOM
 * 			renamable == N/A
 * 			declarable == NO
 * 
 * 		2) operation			newBottom
 * 			label == null
 * 			obj == OpNode
 * 			status == BOTTOM
 * 			renamable == N/A
 * 			declarable == NO
 * 
 * 
 * 		1) hole or param		newBottom
 * 			lhsidx == null
 * 			label == name of the variable
 * 			obj == label
 * 			status == BOTTOM
 * 			renamable == YES for param, NO for hole
 * 			declarable == YES
 * 
 * 		3) LHS variable			newBottom
 * 			lhsidx != null
 * 			label == variable name (no SSA number)
 * 			obj == label
 * 			status == BOTTOM
 * 			renamable == YES
 * 			declarable == YES
 * 
 * 
 * 
 * LIST
 * 
 * @author Lexin Shan
 * @email lshan@eecs.berkeley.edu
 *
 */
public class NodeToSmtValue extends TypedValue {
	
	protected ISuffixSetter suffixSetter;
	protected int hashCode;
	
	/*
	 * Getters and Setters
	 */	

	public void setSuffixSetter(ISuffixSetter setter) {
		this.suffixSetter = setter;
	}
	
	public static OpNode newBottom(Type realType, int numBits, OpCode opCode, NodeToSmtValue...operands) {
		return new OpNode(realType, numBits, opCode, operands);
	}
	
	public static LabelNode newLabel(Type realType, int numBits, String label) {
		return new LabelNode(realType, numBits, label);
	}
	
	public static FuncNode newFuncCall(Type realRetType, int numBits, String funcName,
			NodeToSmtValue...args) {
		return new FuncNode(realRetType, numBits, funcName, args);
	}
	
	public static VarNode newStateDefault(String label, Type realType, int numBits, int rhsIdx) {
		return new VarNode(label, realType, numBits, rhsIdx);
	}
	
	public static VarNode newStateArrayEleDefault(String label, Type realType, int numBits, int rhsIdx) {
		return new VarNode(label, realType, numBits, rhsIdx);
	}
	
	/**
	 * Boolean Constructor
	 * @param c
	 */
	public static NodeToSmtValue newBool(boolean c) {
		return new ConstNode(TypePrimitive.booltype, 1, c ? 1: 0);
	}
	
	/**
	 * Int Constructor
	 * @param c
	 */
	public static NodeToSmtValue newInt(int c, int numBits) {
//		int maxRepresentable = (1 << numBits) - 1;
//		if (numBits == 32) {
//			maxRepresentable = -1;
//		}		
//		c = c & maxRepresentable;
		
		return new ConstNode(TypePrimitive.inttype, numBits, c);
	}
	
	/**
	 * Bit Constructor
	 * @param c
	 */
	public static NodeToSmtValue newBit(int c) {
		return new ConstNode(TypePrimitive.bittype, 1, c);
	}
	
	/**
	 * Constant bit array constructor
	 * @param intVal
	 * @param size
	 * @return
	 */
	public static NodeToSmtValue newBitArray(int intVal, int size) {
		TypeArray bitArrType = new TypeArray(TypePrimitive.bittype, new ExprConstInt(size));
		return new ConstNode(bitArrType, size, intVal);
	}

	
	/**
	 * Hole Constructor, in synthesis phase
	 * @param label
	 */
	public static VarNode newHole(String label, Type holeType, int numBits) {
		return new VarNode(label, holeType, numBits, -1);
	}
	
	/**
	 * Parameter Constructor, in verification phase
	 * @param label
	 */
	public static VarNode newParam(String label, Type paramType, int numBits) {
		return new VarNode(label, paramType, numBits, -1);
	}
	
	/**
	 * List value
	 * @param label
	 */
	public static NodeToSmtValue newList(NodeToSmtValue[] obj) {
		int size = obj.length;
		Type elemType = obj[0].getType();
		return new OpNode(new TypeArray(elemType, new ExprConstInt(size)), 
				size*obj[0].getNumBits(), OpCode.ARRNEW, obj);
	}
	
	/**
	 * List value
	 * @param val
	 * @return
	 */
	public static NodeToSmtValue newListOf(NodeToSmtValue val, int size) {
		NodeToSmtValue[] l = new NodeToSmtValue[size];
		
		for (int i = 0; i < size; i++) {
			l[i] = val;
		}
		return newList(l);
	}

	
	/**
	 * newLHSValue constructor
	 * @param name
	 * @param t
	 * @param lhsidx
	 */
	public NodeToSmtValue(String name, Type t, SmtStatus status, int numBits, Object obj) {
		super(name, SmtType.create(t, numBits), status, obj);
	}
	

	/**
	 * List constructor
	 * @param obj
	 */
	public NodeToSmtValue(List<abstractValue> obj) {
		super(obj);
		NodeToSmtValue firstElm = (NodeToSmtValue) obj.get(0);
		setType(SmtType.create(
				new TypeArray(firstElm.getType(), new ExprConstInt(obj.size())),
				-1));
	}	

	/*
	 * public methods
	 */	

	public boolean isLabel() {
		return false;
	}

	
	public SmtType getSmtType() {
		return (SmtType) this.type;
	}
	
	@Override
	public Type getType() {
		return getSmtType().getRealType();
	}

	
	public int getNumBits() {
		return getSmtType().getNumBits();
	}
	
	public Object accept(FormulaVisitor fv) {
	    return null;
	}
}
