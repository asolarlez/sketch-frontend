package streamit.frontend.spin;

import java.util.ArrayList;
import java.util.List;

import streamit.frontend.experimental.preprocessor.PreprocessSketch;
import streamit.frontend.nodes.Function;
import streamit.frontend.nodes.StreamSpec;
import streamit.frontend.tosbit.SelectFunctionsToAnalyze;
import streamit.frontend.tosbit.recursionCtrl.RecursionControl;
import streamit.frontend.tosbit.recursionCtrl.ZeroInlineRControl;

public class SimpleCleanup extends PreprocessSketch {

	public SimpleCleanup(){
		super(null, 1, new ZeroInlineRControl());
	}
	
	public String transName(String name){
		return name;
	}
	
    protected List<Function> functionsToAnalyze(StreamSpec spec){
	    return new ArrayList<Function>(spec.getFuncs());
    }
	
	@Override
	public Object visitFunction(Function f){
		
		if(f.getName().contains("_fork_thread_")){
			return super.visitFunction(f);
		}		
		return f;
	}
	
}
