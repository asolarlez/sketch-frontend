package sketch.compiler.dataflow.nodesToSB;

import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.ast.core.typs.TypeStructRef;
import sketch.compiler.dataflow.abstractValue;
import sketch.compiler.dataflow.abstractValueType;
import sketch.compiler.dataflow.varState;

public class IntState extends varState {

	
	IntState(Type t){
		super(t);
	}
	
	IntState(Type t, abstractValueType vtype){		
		super(t);
		if( t instanceof  TypePrimitive){
			init( newLHSvalue() );			
		} else
		
		if( t instanceof TypeArray  ){
			TypeArray tarr = (TypeArray) t;
			abstractValue av = typeSize(tarr, vtype); 
			if( av.hasIntVal() ){
				int arrsz = av.getIntVal();
				init( arrsz );
			}else{	
			    init(-1);
				// init( newLHSvalue() );
			}
		} else
        if (t instanceof TypeStructRef) {
			init( newLHSvalue() );	
		}else{
			assert false :"This shouldn't happen";
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
        return new IntAbsValue("LHS", false);
	}

	@Override
	public abstractValue newLHSvalue(int i) {
        return new IntAbsValue("LHS", false);
	}

}
