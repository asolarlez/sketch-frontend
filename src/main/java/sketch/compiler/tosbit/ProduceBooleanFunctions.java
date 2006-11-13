package streamit.frontend.tosbit;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import streamit.frontend.nodes.FieldDecl;
import streamit.frontend.nodes.FuncWork;
import streamit.frontend.nodes.Function;
import streamit.frontend.nodes.Parameter;
import streamit.frontend.nodes.Program;
import streamit.frontend.nodes.Statement;
import streamit.frontend.nodes.StreamSpec;
import streamit.frontend.nodes.StreamType;
import streamit.frontend.nodes.TempVarGen;
import streamit.frontend.nodes.TypePrimitive;
import streamit.frontend.tosbit.recursionCtrl.RecursionControl;

public class ProduceBooleanFunctions extends NodesToSBit {

	public ProduceBooleanFunctions(StreamSpec ss, TempVarGen varGen,
			ValueOracle oracle, PrintStream out,
            int maxUnroll, RecursionControl rcontrol) {
		super(ss, varGen, oracle, out, maxUnroll, rcontrol);
	}
	
	
    public Object visitStreamSpec(StreamSpec spec)
    {    	
        out.print("// " + spec.getContext() + "\n"); 
                        
        state.pushLevel();
        if (spec.getName() != null)
        {	            
            // This is only public if it's the top-level stream,
            // meaning it has type void->void.        	                
            if (spec.getType() == StreamSpec.STREAM_FILTER)
            {
                out.print("Filter");
            }else
            	assert false :"WHAAAT?";
            String nm = spec.getName();   
            out.print(" " + nm + "\n");            
        }                	        	        
        
                        

        // At this point we get to ignore wholesale the stream type, except
        // that we want to save it.
        
        StreamSpec oldSS = ss;
        ss = spec;
        out.print("{\n"); 
        // Output field definitions:
        
        
        additInit = new LinkedList<Statement>();
        
        for (Iterator iter = spec.getVars().iterator(); iter.hasNext(); )
        {
            FieldDecl varDecl = (FieldDecl)iter.next();
            varDecl.accept(this);
        }
        preFil.push("");    		
        // Output method definitions:
        Function f = spec.getFuncNamed("init");
		if( f!= null)
			f.accept(this);
		
		SelectFunctionsToAnalyze funSelector = new SelectFunctionsToAnalyze();
	    List<Function> funcs = funSelector.selectFunctions(spec);
		
		
        for (Iterator<Function> iter = funcs.iterator(); iter.hasNext(); ){
        	f = iter.next();
        	if( ! f.getName().equals("init") ){
        		f.accept(this);
        	}
        }
        
        ss = oldSS;
        
        out.print("}\n");
        state.popLevel();
                
    	
        //assert preFil.size() == 0 : "This should never happen";        
    	
        return null;
    }
    
    
    public Object visitProgram(Program prog)
    {
        // Nothing special here either.  Just accumulate all of the
        // structures and streams.
    		    	
        for (Iterator iter = prog.getStreams().iterator(); iter.hasNext(); ){
        	StreamSpec sp = (StreamSpec)iter.next();	        	
        	funsWParams.put(sp.getName(), sp);	        		        	
        }
        for (Iterator iter = prog.getStreams().iterator(); iter.hasNext(); ){
        	StreamSpec sp = (StreamSpec)iter.next();
        	Function finit = sp.getFuncNamed("init");
        	if(finit == null){
        		sp.accept(this);
        	}else{
	        	if(finit.getParams().size() > 0){
	        		//funsWParams.put(sp.getName(), sp);
	        	}else{	        		
	        		sp.accept(this);
	        	}	
        	}
        }
     
        //assert preFil.size() == 0 : "This should not happen";     
        
        return null;
    }
}
