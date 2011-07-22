package sketch.compiler.dataflow;

import java.util.LinkedList;
import java.util.List;

import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.NameResolver;
import sketch.compiler.ast.core.StreamSpec;
import static sketch.util.Misc.nonnull;

public class SelectFunctionsToAnalyze {
	public SelectFunctionsToAnalyze() {
		super();
	}

    public List<Function> selectFunctions(StreamSpec spec, NameResolver nres) {
		List<Function> result = new LinkedList<Function>();
        for (Function oldFunc : spec.getFuncs()) {
           String specname = oldFunc.getSpecification();
           if( specname != null  ){
        	   result.add(oldFunc);
                result.add(nonnull(nres.getFun(specname),
        	           "function named " + specname + " not found."));
           }
        }
		return result;
	}
}
