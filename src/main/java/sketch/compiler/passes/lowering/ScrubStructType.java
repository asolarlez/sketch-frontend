package streamit.frontend.passes;

import streamit.frontend.nodes.FEReplacer;
import streamit.frontend.nodes.TypePrimitive;
import streamit.frontend.nodes.TypeStruct;
import streamit.frontend.nodes.TypeStructRef;

public class ScrubStructType extends FEReplacer {
	
	 public Object visitTypeStruct (TypeStruct ts) {
	    	return TypePrimitive.inttype;	    	
	 }
	 public Object visitTypeStructRef (TypeStructRef t) {
	    return  TypePrimitive.inttype ;
	 }

}
