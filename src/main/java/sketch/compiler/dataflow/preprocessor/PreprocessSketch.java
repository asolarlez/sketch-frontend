package streamit.frontend.experimental.preprocessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import streamit.frontend.experimental.PartialEvaluator;
import streamit.frontend.experimental.abstractValue;
import streamit.frontend.experimental.MethodState.ChangeTracker;
import streamit.frontend.experimental.nodesToSB.IntVtype;
import streamit.frontend.nodes.ExprArrayRange;
import streamit.frontend.nodes.ExprConstInt;
import streamit.frontend.nodes.ExprFunCall;
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
import streamit.frontend.nodes.ExprArrayRange.RangeLen;
import streamit.frontend.tosbit.recursionCtrl.RecursionControl;

public class PreprocessSketch extends PartialEvaluator {

	public Map<String, Function> newFuns;
	
	
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

	
	
	public Object visitStmtFor(StmtFor stmt)
    {
    	state.pushLevel();
    	Statement ninit = null;
		Expression ncond = null;
		Statement nincr = null;
		Statement nbody = null;
    	try{    		
	        if (stmt.getInit() != null)
	            ninit = (Statement) stmt.getInit().accept(this);
	        boolean goOn = true;
	        int iters = 0;	
	        while(goOn){
	        	state.pushChangeTracker(null, false);
	        	boolean lisReplacer = isReplacer;
	        	isReplacer = false;
	        	abstractValue vcond = (abstractValue) stmt.getCond().accept(this);
	        	if(!vcond.isBottom() && vcond.getIntVal() == 0){
	        		isReplacer = lisReplacer;	        		
	        		break;
	        	}
	        	ChangeTracker ct = null;
	        	try{
	        		stmt.getBody().accept(this);	        	
		        	if (stmt.getIncr() != null){
			        	stmt.getIncr().accept(this);
		        	}
	        	}finally{
	        		ct = state.popChangeTracker();	
	        		isReplacer = lisReplacer;
	        	}
	        	
	        	state.pushChangeTracker(null, false);
	        	ChangeTracker ct2 = state.popChangeTracker();
	        	
	        	goOn = !state.compareChangeTrackers(ct, ct2);
	        	
	        	state.procChangeTrackers(ct, ct2);	        		        	
	        	++iters;
	        }
	        stmt.getCond().accept(this);
	        ncond = exprRV;
	        nbody = (Statement) stmt.getBody().accept(this);
	        if (stmt.getIncr() != null){
	        	nincr = (Statement) stmt.getIncr().accept(this);
        	}
    	}finally{
    		state.popLevel();	        	
    	}
    	if(nbody == null) return stmt;
    	return new StmtFor(stmt.getCx(), ninit, ncond, nincr, nbody);
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
