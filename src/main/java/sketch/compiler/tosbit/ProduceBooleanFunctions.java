package streamit.frontend.tosbit;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import streamit.frontend.nodes.FieldDecl;
import streamit.frontend.nodes.FuncWork;
import streamit.frontend.nodes.Function;
import streamit.frontend.nodes.Parameter;
import streamit.frontend.nodes.StreamSpec;
import streamit.frontend.nodes.StreamType;
import streamit.frontend.nodes.TempVarGen;
import streamit.frontend.nodes.TypePrimitive;

public class ProduceBooleanFunctions extends NodesToSBit {

	public ProduceBooleanFunctions(StreamSpec ss, TempVarGen varGen,
			ValueOracle oracle) {
		super(ss, varGen, oracle);
	}
	
	
    public Object visitStreamSpec(StreamSpec spec)
    {
        String result = "// " + spec.getContext() + "\n"; 
                        
        state.pushLevel();
        if (spec.getName() != null)
        {	            
            // This is only public if it's the top-level stream,
            // meaning it has type void->void.        	                
            if (spec.getType() == StreamSpec.STREAM_FILTER)
            {
                result += "Filter";
            }else
            	assert false :"WHAAAT?";
            String nm = spec.getName();   
            result += " " + nm + "\n";            
        }                	        	        
        
                        

        // At this point we get to ignore wholesale the stream type, except
        // that we want to save it.
        
        StreamSpec oldSS = ss;
        ss = spec;
        result += "{\n"; 
        // Output field definitions:
        
        
        additInit = new LinkedList();
        
        for (Iterator iter = spec.getVars().iterator(); iter.hasNext(); )
        {
            FieldDecl varDecl = (FieldDecl)iter.next();
            result += (String)varDecl.accept(this);
        }
        preFil.push("");    		
        // Output method definitions:
        Function f = spec.getFuncNamed("init");
		if( f!= null)
			result += (String)(f.accept(this));
		
		SelectFunctionsToAnalyze funSelector = new SelectFunctionsToAnalyze();
	    List<Function> funcs = funSelector.selectFunctions(spec);
		
		
        for (Iterator<Function> iter = funcs.iterator(); iter.hasNext(); ){
        	f = iter.next();
        	if( ! f.getName().equals("init") ){
        		result += (String)(f.accept(this));
        	}
        }
//  TODO: DOTHIS      if( spec.getType() == StreamSpec.STREAM_TABLE ){
//        	outputTable=;
//        }
        ss = oldSS;
        
        result += "}\n";
        state.popLevel();
        if (spec.getName() != null){
	        while( preFil.size() > 0){
	        	String otherFil = (String) preFil.pop();
	        	result = otherFil + result;
	        }
        }
        return result;
    }
	

}
