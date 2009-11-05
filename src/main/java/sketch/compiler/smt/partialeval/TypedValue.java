package sketch.compiler.smt.partialeval;

import java.util.LinkedList;
import java.util.List;

import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.dataflow.abstractValue;
import sketch.util.Pair;

/**
 * There are a few things we need to track here.
 * 
 * 1) whether it's a CONST, BOTTOM or JOIN
 * 2) the type of the value, BIT, BITARRAY, BOOL, INT or LIST
 * 3) the number of bits
 * 
 * Things to represent:
 * CONST INT
 * CONST BIT
 * CONST BOOL
 * CONST BITARRAY
 * 
 * BOTTOM var/formula
 * JOIN INT
 * JOIN BIT
 * JOIN BOOL
 * 
 * var1 = const; // normal assignment
 * 		var1 becomes CONST
 *  
 * var1 = var2;
 * 		var1 becomes BOTTOM 
 * 		for bit-array constants, the obj field stores a List<abstractValue>
 * 		for bit-array bottom, the obj field is the name of the var
 * 
 * var1 = (cond)? var2 : var3;
 * 		var1 becomes a JOIN, obj = Pair<List<TypedValue>, List<TypedValue>>
 * 		the first list is a list of conditions
 * 		the second list is a list of values 
 * 
 * var1 = join;
 * 		var1 becomes BOTTOM, obj = null
 * 
 * var1 = var2[var3];
 *		var1 becomes BOTTOM, obj = null
 *
 * var1[var2] = var3;
 * 		create a new BOTTOM value for it, obj = "var1_ssanum"
 * 
 * 
 * @author Lexin Shan
 * @email lshan@eecs.berkeley.edu
 *
 */
public class TypedValue extends abstractValue {

	public enum SmtStatus {
		CONST, BOTTOM, JOIN, LIST,
	}
	
	protected SmtStatus smtStatus = SmtStatus.BOTTOM;
	protected Type type = null;
	
	protected Object obj = null;
	protected String name; // name is only used when it is LHS.

	protected void setType(Type t) {
		
		// TODO clean this up, much of it is unnecessary
		if (t == null) {
			this.type = null;
			return;
		}
		if (t.equals(TypePrimitive.nulltype))
			t = TypePrimitive.inttype;
		
		this.type = t;
		
	}
	
	public Type getType() {
		return this.type;
	}
	
	public String getName() {
		return name;
	}
	
	public Object getObj() {
		return this.obj;
	}
	
	/**
	 * This method should work for any constant value.
	 * for a bit type, return 0 or 1
	 * for a bool type, return 0 or 1
	 * for a int type, return the int value
	 * 
	 * The reason for this special behavior is the PartialEvaluator class assumes
	 * everything has an int value. It does shortcircuit evaluation based on those
	 * int values.
	 */
	@Override
	public int getIntVal() {
		assert isConst() : "getting int value from a non-const";
		return ((Integer) this.obj).intValue();
	}
	
	public boolean getBoolVal() {
		assert this.type.equals(TypePrimitive.booltype) : "using a non-bool as bool";
		return ((Integer) this.obj).intValue() != 0 ? true : false;
	}
	
	/*
	 * This can be called on either CONST or LIST
	 * (non-Javadoc)
	 * @see streamit.frontend.experimental.abstractValue#getVectValue()
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<abstractValue> getVectValue() {
		return (List<abstractValue>) obj;
	}
	
	@Override
	public boolean hasIntVal() {
		return isConst() ? true: false;
	}

	
	@Override
	public boolean isBottom() {
		return smtStatus == SmtStatus.BOTTOM;
	}

	@Override
	public boolean isVect() {
		return smtStatus == SmtStatus.LIST;
	}
	
	public boolean isConst() {
		return smtStatus == SmtStatus.CONST;
	}
	
	
	public boolean isInt() {
		return getType().equals(TypePrimitive.inttype);
	}
	
	public boolean isBit() {
		return getType().equals(TypePrimitive.bittype);
	}
	
	public boolean isBitArray() {
		return BitVectUtil.isBitArray(getType());
	}
	
	public boolean isBool() {
		return getType().equals(TypePrimitive.booltype);
	}
	
	public static TypedValue newBottom(String label, Type t) {
		return new TypedValue(label, t, SmtStatus.BOTTOM,  label);
	}
	
	public static TypedValue newInt(int v) {
		return new TypedValue(null, TypePrimitive.inttype, SmtStatus.CONST, v);
	}
	
	public static TypedValue newBool(boolean v) {
		return new TypedValue(null, TypePrimitive.booltype, SmtStatus.CONST, v ? 1: 0);
	}
	
	public static TypedValue newBit(int v) {
		return new TypedValue(null, TypePrimitive.bittype, SmtStatus.CONST, v);
	}
	
	public static TypedValue newBitArray(int val, int size) {
		TypeArray bitArrType = new TypeArray(TypePrimitive.bittype, new ExprConstInt(size));
		List<abstractValue> list = new LinkedList<abstractValue>();
		for (int i = 0; i < size; i++)
			list.add(newBit(val));
		return new TypedValue(null, bitArrType, SmtStatus.CONST, list);
	}
	
	public static TypedValue newParam(String label, Type t) {
		return new TypedValue(label, t, SmtStatus.BOTTOM, label);
	}
	
	public static TypedValue newHole(String label, Type t) {
		return new TypedValue(label, t, SmtStatus.BOTTOM, label);
	}
	
	public static TypedValue newList(List<abstractValue> obj) {
		return new TypedValue(obj);
	}
	
	
	/**
	 * Copy Constructor
	 * @param n
	 */
	public TypedValue(TypedValue n) {
		this(n.name, n.type, n.smtStatus, n.obj);
	}

	
	public TypedValue (List<abstractValue> conds, List<abstractValue> vals) {
		assert conds.size() > 0 : "join conditions can be empty";
		assert vals.size() > 1 : "join values can be empty";
		assert conds.size() + 1 == vals.size() : "join conditions and values are inconsistent";

		// TODO this is probably wrong
		setType(((TypedValue) vals.get(0)).getType());
		this.obj = null;
		this.name = null;
		this.smtStatus = SmtStatus.JOIN;
		
		Pair<List<abstractValue>, List<abstractValue>> condAndVal = 
			new Pair<List<abstractValue>, List<abstractValue>>(conds, vals);
		this.obj = condAndVal;
	}
	
	/**
	 * List constructor
	 * @param obj
	 */
	public TypedValue(List<abstractValue> obj) {
		this(null, null, SmtStatus.LIST, obj);
		Type eleType = null;
		// take a value out of the list and assign that type to this value
		for (abstractValue av : obj) {
			TypedValue ntsv = (TypedValue) av;
			eleType = ntsv.getType();
			break;
		}
		setType(new TypeArray(eleType, new ExprConstInt(obj.size())));
	}

	protected TypedValue(String name, Type t, SmtStatus status, Object obj) {
		setType(t);
	
		this.obj = obj;
		this.name = name;
		this.smtStatus = status;
		
	}
	
	public abstractValue clone() {
		return new TypedValue(this);
	}

	@Override
	public void update(abstractValue v) {
		TypedValue ntsv = (TypedValue) v;
		
		this.obj = ntsv.obj;
		
//		this.type = ntsv.type;
		
		if (ntsv.isConst()) {
			this.smtStatus = SmtStatus.CONST;	
		} else {
			this.smtStatus = SmtStatus.BOTTOM;
		}
		
	}
	
}
