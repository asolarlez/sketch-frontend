package sketch.compiler.dataflow;

import static sketch.util.Misc.nonnull;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.StreamSpec;

public class SelectFunctionsToAnalyze {
	public SelectFunctionsToAnalyze() {
		super();
	}
	public List<Function> selectFunctions(StreamSpec spec){
		List<Function> result = new LinkedList<Function>();
		for (Iterator iter = spec.getFuncs().iterator(); iter.hasNext(); )
        {
           Function oldFunc = (Function)iter.next();
           String specname = oldFunc.getSpecification();
           if( specname != null  ){
        	   result.add(oldFunc);
        	   result.add(nonnull(spec.getFuncNamed(specname),
        	           "function named " + specname + " not found."));
           }
        }
		return result;
	}
}
