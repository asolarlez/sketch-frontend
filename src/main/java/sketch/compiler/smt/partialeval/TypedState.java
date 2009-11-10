package sketch.compiler.smt.partialeval;

import java.util.List;

import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypeStruct;
import sketch.compiler.ast.core.typs.TypeStructRef;
import sketch.compiler.dataflow.abstractValue;
import sketch.compiler.dataflow.abstractValueType;
import sketch.compiler.dataflow.varState;

public class TypedState extends varState {

	protected String name;

	public TypedState(Type t) {
		super(t);
	}

	public TypedState(String name, Type t, TypedVtype vtype) {
		super(t);
		this.name = name;

		if (BitVectUtil.isPrimitive(t)) {
			init(defaultLHSValue());
		} else if (t instanceof TypeArray) {
			TypeArray tarr = (TypeArray) t;
			abstractValue av = typeSize(tarr, vtype);
			if (av.hasIntVal()) {
				int arrsz = av.getIntVal();
				init(arrsz);
			} else {
				init(-1);
			}
		} else if (t instanceof TypeStructRef || t instanceof TypeStruct) {
			init(defaultLHSValue());
		}

	}


	@Override
	public abstractValue newLHSvalue() {
		return defaultLHSValue();

	}

	private abstractValue defaultLHSValue() {
		return TypedValue.newBottom(name, getType());
	}

	@Override
	public TypedValue newLHSvalue(int i) {
		Type stateType = getType();
		if (stateType instanceof TypeArray) {
			// This is called to create the NodeToSmtValue object for elements
			// in the array
			t = ((TypeArray) stateType).getBase();
		} else {
			t = stateType;
		}

		return TypedValue.newBottom(name + "_idx_" + i, getType());
	}

	@Override
	public varState getDeltaClone(abstractValueType vt) {
		TypedState st = new TypedState(getType());
		st.helperDeltaClone(this, vt);
		return st;
	}

	@Override
	public void update(abstractValue idx, abstractValue val,
			abstractValueType vtype) {
		TypedValue tVal = (TypedValue) val;

		assert arrElems != null;
		if (arrElems == null) {
			if (absVal.isVect()) {
				assert false : "NYI";
			} else {
				absVal.update(vtype.BOTTOM());
			}
		}
		if (idx.hasIntVal()) {
			int iidx = idx.getIntVal();
			if (arrElems.containsKey(iidx)) {
				arrElems.get(iidx).update(val);
			} else {
				abstractValue newVal = newLHSvalue(iidx);
				if (parent != null) {
					abstractValue tmp = state(iidx);
					if (tmp.isVolatile()) {
						newVal.makeVolatile();
					}
				}
				arrElems.put(iidx, newVal);
				newVal.update(val);
			}
		} else {
			abstractValue bottom = vtype.BOTTOM(tVal.getType());
			int lv = this.numKeys();
			for (int i = 0; i < lv; ++i) {
				// update(vtype.CONST(i), bottom, vtype);
				if (arrElems.containsKey(i)) {
					arrElems.get(i).update(bottom); // This could be more
													// precise by doing a
													// condjoin between the
													// current value and
													// prevvalue on cond (idx ==
													// i).
				} else {
					abstractValue newVal = newLHSvalue(i);
					if (parent != null) {
						abstractValue tmp = state(i);
						if (tmp.isVolatile()) {
							newVal.makeVolatile();
						}
					}
					arrElems.put(i, newVal);
					newVal.update(bottom);
				}
			}
		}
	}

	@Override
	public void update(abstractValue val, abstractValueType vtype) {

		if (val.isVect()) {
			if (arrElems == null) {
				init(val.getVectValue().size());
	
			}
			
			int lv = this.numKeys();
			List<abstractValue> vlist = val.getVectValue();
			for (int i = 0; i < lv; ++i) {
				abstractValue cv;
				if (i < vlist.size()) {
					cv = vlist.get(i);
				} else {
					cv = vtype.CONST(0);
				}
				update(vtype.CONST(i), cv, vtype);
			}
			
		} else {
			arrElems = null;
			if (absVal == null)
				absVal = val;
			else 
				absVal.update(val);
		}
	}

}
