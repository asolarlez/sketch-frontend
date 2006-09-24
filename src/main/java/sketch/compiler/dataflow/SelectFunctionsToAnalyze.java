package streamit.frontend.tosbit;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import streamit.frontend.nodes.Function;
import streamit.frontend.nodes.StreamSpec;

public class SelectFunctionsToAnalyze {
	public SelectFunctionsToAnalyze() {
		super();
	}
	public List<Function> selectFunctions(StreamSpec spec){
		List<Function> result = new ArrayList<Function>();
		for (Iterator iter = spec.getFuncs().iterator(); iter.hasNext(); )
        {
           Function oldFunc = (Function)iter.next();
           String specname = oldFunc.getSpecification();
           if( specname != null  ){
        	   result.add(oldFunc);
        	   result.add(spec.getFuncNamed(specname));
           }
        }
		return result;
	}
}
