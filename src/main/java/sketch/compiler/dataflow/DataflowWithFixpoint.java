package sketch.compiler.dataflow;

import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.stmts.StmtFor;
import sketch.compiler.ast.core.stmts.StmtReorderBlock;
import sketch.compiler.dataflow.MethodState.ChangeTracker;
import sketch.compiler.dataflow.MethodState.Level;
import sketch.compiler.dataflow.recursionCtrl.RecursionControl;

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

    /**
     * This method will compute the least fixed point for all the variables in the
     * analysis.
     */
	public Object visitStmtFor(StmtFor stmt)
    {
    	Level lvl = state.pushLevel("dataflowwithfixedpoint for");
    	Statement ninit = null;
		Expression ncond = null;
		Statement nincr = null;
		Statement nbody = null;
    	try{
            /**
             * First, we process the initialization, because that happens unconditionally
             * outside the loop. We call the state after this initialization init, the
             * initial state of the loop body.
             */
	        if (stmt.getInit() != null)
	            ninit = (Statement) stmt.getInit().accept(this);
	        boolean goOn = true;
	        int iters = 0;
            /**
             * In this loop, we first push a change tracker A, and evaluate the condition
             * under A, so A now contains the state cond(init), i.e. the result of
             * evaluating the condition on the intial state init. Then we push a change
             * tracker B, evaluate the body under B and pop B. The changes from B are
             * merged into A, so A now has Join(body(cond(init)), cond(init)). i.e. the
             * join of the state that you get from executing the condition on the initial
             * state and what you get from executing the condition and the body on the
             * initial state. Then we check whether the state in A is the same as the
             * initial state, and we join A with the initial state. If A was the same,
             * then we have reached a fixed point and the join will have no effect. If we
             * have not reached a fixed point, the process will be repeated with the new
             * initial state which corresponds to the join of the previous initial state
             * and the current state.
             */
	        while(goOn){
	        	ChangeTracker changed;	        	
	        	try{
                    state.pushChangeTracker(null, false); // pushing CT A.
		        	ChangeTracker ct = null;

		        	boolean lisReplacer = isReplacer;
		        	isReplacer = false;
		        	abstractValue vcond = (abstractValue) stmt.getCond().accept(this);
		        	if(vcond.hasIntVal() && vcond.getIntVal() == 0){
		        		isReplacer = lisReplacer;
		        		break;
		        	}
		        	try{
                        state.pushChangeTracker(vcond, false);// pushing ct B.
		        		Object ka =stmt.getBody().accept(this);
			        	if (stmt.getIncr() != null){
				        	stmt.getIncr().accept(this);
			        	}
		        	}finally{
		        		ct = state.popChangeTracker();
		        		isReplacer = lisReplacer;
		        	}
                    state.procChangeTrackers(ct);// Merging B into A
	        	}finally{
                    changed = state.popChangeTracker(); // Poping A.
	        	}
                state.pushChangeTracker(null, false); // Pushing C.
	        	ChangeTracker orig = state.popChangeTracker();
	        	goOn = !state.compareChangeTrackers(changed, orig);
	        	state.procChangeTrackers(changed, orig);
	        	++iters;
	        	if(iters > 5000){
	        		throw new RuntimeException("Infinite loop detected: " + stmt);
	        	}
	        }
            /**
             * At this point, the state now corresponds to the fixed point state at the
             * beginning of the loop. We must then evaluate the body of the loop under
             * this state, and compute the state at the end of the loop by merging the
             * resulting state with the state at the beginning.
             */
            ChangeTracker ct;
            try {
                state.pushChangeTracker(null, false); // Pushing Final CT.
                abstractValue vcond = (abstractValue) stmt.getCond().accept(this);
                ncond = exprRV;
                // printDebug("DataflowWithFixedpoint last stack elt 1",
                // state.getLevelStack().peek());
                if (vcond.hasIntVal() && vcond.getIntVal() == 0) {
                    nbody = null;
                } else {
                    nbody = (Statement) stmt.getBody().accept(this);
                    if (stmt.getIncr() != null) {
                        nincr = (Statement) stmt.getIncr().accept(this);
                    }
                }
            } finally {
                ct = state.popChangeTracker();
	        }
            state.procChangeTrackers(ct);
	        // printDebug("DataflowWithFixedpoint last stack elt 2", state.getLevelStack().peek());
    	} catch(Exception ex) {
    	    // printDebug("DataflowWithFixedpoint encountered error", ex.getMessage());
    	}finally{
    	    // printDebug("DataflowWithFixedpoint last stack elt 3", state.getLevelStack().peek());
    		state.popLevel(lvl);
    	}
    	if(nbody == null) return null;
    	return isReplacer?  new StmtFor(stmt, ninit, ncond, nincr, nbody) : stmt;
    }
}
