package streamit.frontend.experimental.preprocessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import streamit.frontend.experimental.DataflowWithFixpoint;
import streamit.frontend.experimental.PartialEvaluator;
import streamit.frontend.experimental.abstractValue;
import streamit.frontend.experimental.MethodState.ChangeTracker;
import streamit.frontend.experimental.nodesToSB.IntVtype;
import streamit.frontend.nodes.ExprArrayRange;
import streamit.frontend.nodes.ExprConstInt;
import streamit.frontend.nodes.ExprFunCall;
import streamit.frontend.nodes.ExprStar;
import streamit.frontend.nodes.ExprVar;
import streamit.frontend.nodes.Expression;
import streamit.frontend.nodes.FEContext;
import streamit.frontend.nodes.Function;
import streamit.frontend.nodes.Parameter;
import streamit.frontend.nodes.Statement;
import streamit.frontend.nodes.StmtAssert;
import streamit.frontend.nodes.StmtAssign;
import streamit.frontend.nodes.StmtBlock;
import streamit.frontend.nodes.StmtFor;
import streamit.frontend.nodes.StmtVarDecl;
import streamit.frontend.nodes.TempVarGen;
import streamit.frontend.nodes.Type;
import streamit.frontend.nodes.ExprArrayRange.RangeLen;
import streamit.frontend.tosbit.recursionCtrl.RecursionControl;

/**
 * 
 * The sketch preprocessor mainly does constant propagation and inlining of functions and unrolling of loops.
 * After this step, all the holes are now regarded as static holes.
 * @author asolar
 *
 */
public class PreprocessSketch extends DataflowWithFixpoint {

	public Map<String, Function> newFuns;
	
	
	 public Object visitExprStar(ExprStar star) {
		 Object obj = super.visitExprStar(star);
		 ExprStar old = (ExprStar)exprRV;
		 exprRV = new ExprStar(old);
		 return obj;
		}
	
	
	@Override
	public String transName(String name){
		return state.transName(name);
	}
	
	public PreprocessSketch(TempVarGen vargen, int maxUnroll, RecursionControl rcontrol){
		super(new IntVtype(), vargen,true, maxUnroll, rcontrol );
		newFuns = new HashMap<String, Function>();
	}
	

	
	
	
	
	
	public Object visitExprFunCall(ExprFunCall exp)
	{
    	String name = exp.getName();
    	// Local function?
		Function fun = ss.getFuncNamed(name);
		if(fun.getSpecification()!= null){
			String specName = fun.getSpecification();
			if( newFuns.containsKey(specName)){
				fun = newFuns.get(specName);
			}else{
				Function newFun = ss.getFuncNamed(specName);
				state.pushLevel();
				fun = (Function)this.visitFunction(newFun);
				state.popLevel();
			}
		}
    	if (fun != null) {   
    		if( fun.isUninterp()  ){    			
    			return super.visitExprFunCall(exp);
    		}else{
	    		if (rcontrol.testCall(exp)) {
	                /* Increment inline counter. */
	            	rcontrol.pushFunCall(exp, fun);  
	    		
					List<Statement>  oldNewStatements = newStatements;
					newStatements = new ArrayList<Statement> ();
					Statement result = null;
					state.pushLevel();
					try{
			    		{
			    			Iterator<Expression> actualParams = exp.getParams().iterator();	        		        	       	
			    			Iterator<Parameter> formalParams = fun.getParams().iterator();
			    			inParameterSetter(formalParams, actualParams, false);
			    		}
			    		Statement body = (Statement) fun.getBody().accept(this);
			    		addStatement(body);
			    		{
			    			Iterator<Expression> actualParams = exp.getParams().iterator();	        		        	       	
			    			Iterator<Parameter> formalParams = fun.getParams().iterator();
			    			outParameterSetter(formalParams, actualParams, false);
			    		}
			    		result = new StmtBlock(exp.getContext(), newStatements);
		    		}finally{
		    			state.popLevel();
		    			newStatements = oldNewStatements;
		    		}
		            addStatement(result);
		    		
		    		rcontrol.popFunCall(exp);
	    		}else{
	    			StmtAssert sas = new StmtAssert(exp.getContext(), new ExprConstInt(0));
	    			addStatement(sas);    			    		
	    		}
	    		exprRV = null;
	    		return vtype.BOTTOM();
    		}
    	}
    	exprRV = null;
    	return vtype.BOTTOM();    	
    }

	
	
	public Object visitFunction(Function func){
		if( newFuns.containsKey(func.getName()) ){
			return newFuns.get(func.getName());
		}
		Function obj = (Function)super.visitFunction(func);
		newFuns.put(obj.getName(), obj);
		return obj;
	}
}
