package streamit.frontend.experimental.cflowChecks;

import streamit.frontend.experimental.abstractValue;
import streamit.frontend.experimental.abstractValueType;
import streamit.frontend.experimental.varState;
import streamit.frontend.nodes.Type;
import streamit.frontend.nodes.TypeArray;
import streamit.frontend.nodes.TypePrimitive;
import streamit.frontend.nodes.TypeStruct;
import streamit.frontend.nodes.TypeStructRef;

public class CfcState extends varState {

	
	CfcState(Type t){
		super(t);
	}
	
	CfcState(Type t, abstractValueType vtype){		
		super(t);
		init(newLHSvalue());
	}
	
	
	@Override
	public varState getDeltaClone(abstractValueType vt) {
		CfcState st  = new CfcState(getType());
		st.helperDeltaClone(this, vt);		
		return st;
	}
	
	
	@Override
	public abstractValue newLHSvalue() {
		return new CfcValue(CfcValue.noinit);
	}

	@Override
	public abstractValue newLHSvalue(int i) {
		return new CfcValue(CfcValue.noinit);
	}

}
