package streamit.frontend.experimental.nodesToSB;

import streamit.frontend.experimental.abstractValue;
import streamit.frontend.experimental.abstractValueType;
import streamit.frontend.experimental.varState;
import streamit.frontend.nodes.Type;
import streamit.frontend.nodes.TypeArray;
import streamit.frontend.nodes.TypePrimitive;

public class IntState extends varState {

	
	IntState(Type t){
		super(t);
	}
	
	IntState(Type t, abstractValueType vtype){		
		super(t);
		if( t instanceof  TypePrimitive){
			init( newLHSvalue() );			
		}
		
		if( t instanceof TypeArray  ){
			TypeArray tarr = (TypeArray) t;
			abstractValue av = (abstractValue) tarr.getLength().accept( vtype.eval );
			if( av.hasIntVal() ){
				int arrsz = av.getIntVal();
				init( arrsz );
			}else{
				init( -1 );
			}
		}
	}
	
	
	@Override
	public varState getDeltaClone(abstractValueType vt) {
		IntState st  = new IntState(getType());
		st.helperDeltaClone(this, vt);		
		return st;
	}

	@Override
	public abstractValue newLHSvalue() {
		return new IntAbsValue("LHS");
	}

	@Override
	public abstractValue newLHSvalue(int i) {
		return new IntAbsValue("LHS");
	}

}
