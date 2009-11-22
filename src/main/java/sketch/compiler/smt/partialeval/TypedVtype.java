package sketch.compiler.smt.partialeval;

import java.util.List;

import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.dataflow.MethodState;
import sketch.compiler.dataflow.abstractValue;
import sketch.compiler.dataflow.varState;
import sketch.compiler.dataflow.nodesToSB.IntVtype;
import sketch.compiler.smt.SMTTranslator;

public class TypedVtype extends IntVtype {

	protected SMTTranslator mTrans;
	protected MethodState state;
	
	public TypedVtype(SMTTranslator trans) {
		mTrans = trans;
	}
	
	// Getters & Setters
	public MethodState getMethodState() {
		return this.state;
	}
	public void setMethodState(MethodState s) {
		this.state = s;
	}
	
	
	@Override
	public varState cleanState(String var, Type t, MethodState mstate) {
		return new TypedState(var, t, this);
	}
	

	@Override
	public abstractValue BOTTOM() {
		throw new IllegalStateException("API bug");
	}
	
	@Override
	public abstractValue BOTTOM(String label) {
		throw new IllegalStateException("API bug");
	}
	
	public TypedValue BOTTOM(String label, Type type) {
		return TypedValue.newBottom(label, type);
	}

	public TypedValue BOTTOM(Type t) {
		return BOTTOM("ARR", t);
	}
	
	@Override
	public TypedValue ARR(List<abstractValue> vals) {
		return TypedValue.newList(vals);
	}

	@Override
	public TypedValue CONST(boolean v) {
		return TypedValue.newBool(v);
	}

	@Override
	public TypedValue CONST(int v) {
		return TypedValue.newInt(v);
	}

	public TypedValue CONSTBIT(int v) {
		return TypedValue.newBit(v);
	}
	
	public TypedValue CONSTBITARRAY(int val, int size) {
		return TypedValue.newBitArray(val, size);
	}
	
	@Override
	public TypedValue NULL() {
		return CONST(-1);
	}
	
	@Override
	public TypedValue STAR(FENode star) {
		return BOTTOM("??", TypePrimitive.inttype);
	}

	
	@Override
	public abstractValue cast(abstractValue v1, Type targetType) {
		TypedValue ntsv1 = (TypedValue) v1;

		// 		bit 			bool 	int
		// bit 	N/A 			b!=bv0 	concat
		// bool b?bv1:bv0 		N/A 	b?1:0
		// int 	i!=0?bv1:bv0 	i != 0 	N/A

		// no op
		if (ntsv1.getType().equals(targetType))
			return v1;
		
		if (targetType == TypePrimitive.nulltype)
			return CONST(-1);

		if (ntsv1.isConst()) {
			if (ntsv1.isBit()) {

				if (targetType.equals(TypePrimitive.booltype))
					// cast from bit to bool
					return ntsv1.getIntVal() != 0 ? CONST(true) : CONST(false);
				else if (targetType.equals(TypePrimitive.inttype))
					// cast from bit to int
					return ntsv1.getIntVal() != 0 ? CONST(1) : CONST(0);

			} else if (ntsv1.isBool()) {
				if (targetType.equals(TypePrimitive.bittype))
					// cast from bool to bit
					return ntsv1.getBoolVal() ? CONSTBIT(1) : CONSTBIT(0);
				else if (targetType.equals(TypePrimitive.inttype))
					// cast from bool to int
					return ntsv1.getBoolVal() ? CONST(1) : CONST(0);
			} else if (ntsv1.isInt()) {
				if (targetType.equals(TypePrimitive.bittype))
					// cast from int to bit
					return ntsv1.getIntVal() != 0 ? CONSTBIT(1) : CONSTBIT(0);
				else if (targetType.equals(TypePrimitive.booltype))
					// cast from int to bool
					return ntsv1.getIntVal() != 0 ? CONST(1) : CONST(0);
			}
		}
		
		return BOTTOM(targetType);
	}
	

	
	
	@Override
	public abstractValue ternary(abstractValue cond, abstractValue vtrue,
			abstractValue vfalse) {
		assert (cond != null) : "API usage bug";
		return condjoin(cond, vtrue, vfalse);
	}

	@Override
	public abstractValue arracc(abstractValue arr, abstractValue idx) {
		assert false : "not expected";
		return super.arracc(arr, idx);
	}

	
	@Deprecated
	protected TypedValue rawArracc(abstractValue arr, abstractValue idx) {
		throw new IllegalStateException("Deprecated function called");
	}
	
	/**
	 * This method is called when the index of an array access it not constant
	 * 
	 * @param arr
	 * @param idx
	 * @param len
	 * @return
	 */
	protected TypedValue rawArracc(TypedValue arr, TypedValue idx, TypedValue len) {
		TypedValue typedVal = (TypedValue) arr;
		return BOTTOM(arr + "[" + idx + "::" + len + "]", typedVal.getType());
	}
	
	public abstractValue outOfBounds(TypedValue arr){
		Type eleType = getElementType(arr.getType());
		return defaultValue(eleType);
				
	}


	@Override
	public void funcall(Function fun, List<abstractValue> avlist,
			List<abstractValue> outSlist, abstractValue pathCond) {
		throw new UnsupportedOperationException("API bug");
	}
	
	public TypedValue defaultValue(Type t) {
		if (t.equals(TypePrimitive.inttype))
			return TypedValue.newInt(0);
		else if (t.equals(TypePrimitive.bittype))
			return TypedValue.newBit(0);
		else
			return TypedValue.newBool(false);
	}
	
	/*
	 * Helper functions
	 */	

	public static Type getElementType(Type arrayType) {
		return ((TypeArray) arrayType).getBase();
	}

}
