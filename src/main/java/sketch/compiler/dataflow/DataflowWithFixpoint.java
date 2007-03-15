package streamit.frontend.experimental;

import streamit.frontend.experimental.MethodState.ChangeTracker;
import streamit.frontend.nodes.Expression;
import streamit.frontend.nodes.Statement;
import streamit.frontend.nodes.StmtFor;
import streamit.frontend.nodes.TempVarGen;
import streamit.frontend.tosbit.recursionCtrl.RecursionControl;

public class DataflowWithFixpoint extends PartialEvaluator {

	public DataflowWithFixpoint(abstractValueType vtype, TempVarGen varGen,  boolean isReplacer, int maxUnroll, RecursionControl rcontrol) {
		super(vtype, varGen, isReplacer, maxUnroll, rcontrol);
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
	        	state.pushChangeTracker(null, false);
	        	boolean lisReplacer = isReplacer;
	        	isReplacer = false;
	        	abstractValue vcond = (abstractValue) stmt.getCond().accept(this);
	        	if(vcond.hasIntVal() && vcond.getIntVal() == 0){
	        		isReplacer = lisReplacer;	        		
	        		break;
	        	}
	        	ChangeTracker ct = null;
	        	try{
	        		stmt.getBody().accept(this);	        	
		        	if (stmt.getIncr() != null){
			        	stmt.getIncr().accept(this);
		        	}
	        	}catch(RuntimeException e){
	        		state.popChangeTracker();
	        		throw e;
	        		//Should also pop the other change tracker.
	        	}finally{
	        		ct = state.popChangeTracker();	
	        		isReplacer = lisReplacer;
	        	}
	        	
	        	state.pushChangeTracker(null, false);
	        	ChangeTracker ct2 = state.popChangeTracker();
	        	state.procChangeTrackers(ct, ct2);	 

	        	ChangeTracker changed = state.popChangeTracker();
	        	state.pushChangeTracker(null, false);	        	
	        	ChangeTracker orig = state.popChangeTracker();
	        	goOn = !state.compareChangeTrackers(changed, orig);
	        	state.procChangeTrackers(changed, orig);
	        	++iters;
	        	if(iters > 5000){
	        		throw new RuntimeException("Infinite loop detected: " + stmt);
	        	}
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
}
