package streamit.frontend.experimental;

import streamit.frontend.experimental.MethodState.ChangeTracker;
import streamit.frontend.nodes.Expression;
import streamit.frontend.nodes.Statement;
import streamit.frontend.nodes.StmtReorderBlock;
import streamit.frontend.nodes.StmtBlock;
import streamit.frontend.nodes.StmtFor;
import streamit.frontend.nodes.StmtFork;
import streamit.frontend.nodes.StmtVarDecl;
import streamit.frontend.nodes.TempVarGen;
import streamit.frontend.tosbit.recursionCtrl.RecursionControl;

public class DataflowWithFixpoint extends PartialEvaluator {

	public DataflowWithFixpoint(abstractValueType vtype, TempVarGen varGen,  boolean isReplacer, int maxUnroll, RecursionControl rcontrol) {
		super(vtype, varGen, isReplacer, maxUnroll, rcontrol);
	}	
	
	public Object visitStmtReorderBlock(StmtReorderBlock block){
		processBlockWithCrazyEffects(block.getBlock());
		StmtBlock newBlock = (StmtBlock) block.getBlock().accept(this);
		return isReplacer ? new StmtReorderBlock(block, newBlock.getStmts() ) : block;		
	}
	
	
	public void processBlockWithCrazyEffects(Statement stmt){		
		 boolean goOn = true;
	        int iters = 0;
	        while(goOn){
	        	state.pushChangeTracker(null, false);
	        	ChangeTracker ct = null;        	
	        	boolean lisReplacer = isReplacer;
	        	isReplacer = false;
	        	try{
	        		state.pushChangeTracker(null, false);
	        		stmt.accept(this);	        	
	        	}catch(RuntimeException e){
	        		state.popChangeTracker();
	        		throw e;
	        		//Should also pop the other change tracker.
	        	}finally{
	        		ct = state.popChangeTracker();	
	        		isReplacer = lisReplacer;
	        	}
	        	state.procChangeTrackersConservative(ct);	 
	
	        	ChangeTracker changed = state.popChangeTracker();
	        	state.pushChangeTracker(null, false);
	        	ChangeTracker orig = state.popChangeTracker();
	        	goOn = !state.compareChangeTrackers(changed, orig);
	        	state.procChangeTrackers(changed, orig);
	        	++iters;
	        	if(iters > 5000){
	        		throw new RuntimeException("Infinite loop detected: " + stmt +":" + stmt);
	        	}
	        }		
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
	        	ChangeTracker changed;
	        	try{
		        	state.pushChangeTracker(null, false);
		        	
		        	ChangeTracker ct = null;
		        	
		        	boolean lisReplacer = isReplacer;
		        	isReplacer = false;
		        	abstractValue vcond = (abstractValue) stmt.getCond().accept(this);	        	
		        	if(vcond.hasIntVal() && vcond.getIntVal() == 0){
		        		isReplacer = lisReplacer;	
		        		state.popChangeTracker();
		        		break;
		        	}	        	
		        	try{
		        		state.pushChangeTracker(vcond, false);
		        		Object ka =stmt.getBody().accept(this);	        	
			        	if (stmt.getIncr() != null){
				        	stmt.getIncr().accept(this);
			        	}
		        	}catch(RuntimeException e){
		        		state.popChangeTracker();
		        		throw e;
		        	}finally{
		        		ct = state.popChangeTracker();	
		        		isReplacer = lisReplacer;
		        	}
		        	state.procChangeTrackers(ct);	 
	        	}finally{
	        		changed = state.popChangeTracker();
	        	}
	        	state.pushChangeTracker(null, false);	        	
	        	ChangeTracker orig = state.popChangeTracker();
	        	goOn = !state.compareChangeTrackers(changed, orig);
	        	state.procChangeTrackers(changed, orig);
	        	++iters;
	        	if(iters > 5000){
	        		throw new RuntimeException("Infinite loop detected: " + stmt);
	        	}
	        }
	        abstractValue vcond = (abstractValue) stmt.getCond().accept(this);
	        ncond = exprRV;
	        if(vcond.hasIntVal() && vcond.getIntVal() == 0){
	        	nbody = null;
	        }else{
		        nbody = (Statement) stmt.getBody().accept(this);
		        if (stmt.getIncr() != null){
		        	nincr = (Statement) stmt.getIncr().accept(this);
	        	}
	        }
    	}finally{
    		state.popLevel();	        	
    	}
    	if(nbody == null) return null;
    	return isReplacer?  new StmtFor(stmt, ninit, ncond, nincr, nbody) : stmt;
    }
}
