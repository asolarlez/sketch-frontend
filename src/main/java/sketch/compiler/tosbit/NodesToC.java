package streamit.frontend.tosbit;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import streamit.frontend.nodes.FieldDecl;
import streamit.frontend.nodes.Function;
import streamit.frontend.nodes.StreamSpec;
import streamit.frontend.nodes.StreamType;
import streamit.frontend.nodes.TempVarGen;
import streamit.frontend.tojava.NodesToJava;

public class NodesToC extends NodesToJava {

	public NodesToC(boolean libraryFormat, TempVarGen varGen) {
		super(libraryFormat, varGen);
		// TODO Auto-generated constructor stub
	}
	
	public Object visitStreamSpec(StreamSpec spec){
		String result = "";
		ss = spec;
        for (Iterator iter = spec.getFuncs().iterator(); iter.hasNext(); )
        {
            Function oldFunc = (Function)iter.next();
            result += (String)oldFunc.accept(this);            
        }
        return result;
	}

}
