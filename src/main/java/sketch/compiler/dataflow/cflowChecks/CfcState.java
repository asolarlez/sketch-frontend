package sketch.compiler.dataflow.cflowChecks;

import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.dataflow.abstractValue;
import sketch.compiler.dataflow.abstractValueType;
import sketch.compiler.dataflow.varState;

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
