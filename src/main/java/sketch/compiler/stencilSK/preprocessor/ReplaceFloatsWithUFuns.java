package sketch.compiler.stencilSK.preprocessor;

import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.typs.TypePrimitive;

public class ReplaceFloatsWithUFuns extends ReplaceFloatsWithBits {

	public static final String addFunName = "ADD_FL";
	public static final String subFunName = "SUB_FL";
	public static final String mulFunName = "MUL_FL";
	public static final String divFunName = "DIV_FL";
	
	
	
	
	public ReplaceFloatsWithUFuns(TempVarGen varGen){
	    super(varGen);
		
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
