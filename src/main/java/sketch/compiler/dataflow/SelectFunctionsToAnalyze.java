package sketch.compiler.dataflow;

import java.util.LinkedList;
import java.util.List;

import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.NameResolver;
import sketch.compiler.ast.core.Package;
import static sketch.util.Misc.nonnull;

public class SelectFunctionsToAnalyze {
	public SelectFunctionsToAnalyze() {
		super();
	}

    public List<Function> selectFunctions(Package spec, NameResolver nres) {
		List<Function> result = new LinkedList<Function>();
        for (Function oldFunc : spec.getFuncs()) {
           String specname = oldFunc.getSpecification();
           if( specname != null  ){
        	   result.add(oldFunc);
                result.add(nonnull(nres.getFun(specname),
        	           "function named " + specname + " not found."));
           }
            String funname = oldFunc.getName();
            if (funname.length() > 9 && funname.substring(0, 9).equals("glblInit_")) {
                result.add(oldFunc);
            }
        }
		return result;
	}
}
