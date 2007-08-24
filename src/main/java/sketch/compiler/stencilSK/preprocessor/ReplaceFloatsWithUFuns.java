package streamit.frontend.stencilSK.preprocessor;

import streamit.frontend.nodes.Function;
import streamit.frontend.nodes.TypePrimitive;

public class ReplaceFloatsWithUFuns extends ReplaceFloatsWithBits {

	public static final String addFunName = "ADD_FL";
	public static final String subFunName = "SUB_FL";
	public static final String mulFunName = "MUL_FL";
	public static final String divFunName = "DIV_FL";
	
	
	
	
	public ReplaceFloatsWithUFuns(){
		
	}
	
	
	Function addFunction(){
		return null;
	}
	
	
	public Object visitTypePrimitive(TypePrimitive t) {
		if(t.equals(FLOAT)){
			return TypePrimitive.inttype;
		}
    	return t;
    }
	
}
