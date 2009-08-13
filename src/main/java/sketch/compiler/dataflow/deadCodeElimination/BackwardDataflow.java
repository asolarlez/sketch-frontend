package sketch.compiler.dataflow.deadCodeElimination;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.stmts.StmtFor;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.promela.stmts.StmtFork;
import sketch.compiler.dataflow.DataflowWithFixpoint;
import sketch.compiler.dataflow.abstractValue;
import sketch.compiler.dataflow.abstractValueType;
import sketch.compiler.dataflow.MethodState.ChangeTracker;
import sketch.compiler.dataflow.recursionCtrl.RecursionControl;


public class BackwardDataflow extends DataflowWithFixpoint {


	public BackwardDataflow(abstractValueType vtype, TempVarGen varGen,  boolean isReplacer, int maxUnroll, RecursionControl rcontrol){
		super(vtype, varGen, isReplacer, maxUnroll, rcontrol);
	}

	/**
	 * Because we are going backwards, then when we visit the actual var declaration,
	 * we don't need to declare the variable anymore. We only care about the initializer.
	 *
	 *
	 */
    public Object visitStmtVarDecl(StmtVarDecl stmt)
    {
    	List<Type> types = isReplacer? new ArrayList<Type>() : null;
    	List<String> names = isReplacer? new ArrayList<String>() : null;
    	List<Expression> inits = isReplacer? new ArrayList<Expression>() : null;
        for (int i = 0; i < stmt.getNumVars(); i++)
        {
            String nm = stmt.getName(i);
            Type vt = (Type)stmt.getType(i).accept(this);
            //Variable declaration not needed.
            Expression ninit = null;
            if( stmt.getInit(i) != null ){
            	abstractValue init = (abstractValue) stmt.getInit(i).accept(this);
            	ninit = exprRV;
            	state.setVarValue(nm, init);
            }
            if( isReplacer ){
            	types.add(vt);
            	names.add(transName(nm));
            	inits.add(ninit);
            }
        }
        return isReplacer? new StmtVarDecl(stmt, types, names, inits) : stmt;
    }

    /**
     * The variable declaration is done separately.
     * @param stmt
     * @return
     */
    public void varDecl(StmtVarDecl stmt)
    {
        for (int i = 0; i < stmt.getNumVars(); i++)
        {
            String nm = stmt.getName(i);
            Type vt = (Type)stmt.getType(i).accept(this);
            state.varDeclare(nm, vt);
        }
    }


    public Object visitStmtBlock(StmtBlock stmt)
    {
    	/**
    	 * Because we are doing dataflow backwards, we need to visit the statements in a
    	 * block in the reverse direction. However, we must first run the variable declarations
    	 * in the block in order for things to work properly.
    	 *
    	 *
    	 */
        // Put context label at the start of the block, too.
    	state.pushLevel();
    	//First, we declare the variables in the block.
    	for (Iterator iter = stmt.getStmts().iterator(); iter.hasNext(); )
        {
            Statement s = (Statement)iter.next();
            // completely ignore null statements, causing them to
            // be dropped in the output
            if (s == null)
                continue;
            if( s instanceof StmtVarDecl  ){
            	varDecl((StmtVarDecl) s);
            }
        }


    	Statement rs = null;
    	try{
    		{
    	        List<Statement> oldStatements = newStatements;
    	        newStatements = new ArrayList<Statement>();
    	        List<Statement> blockBody = stmt.getStmts();
    	        for(int i=blockBody.size()-1; i>=0; --i){
    	        	Statement s = blockBody.get(i);
    	            // completely ignore null statements, causing them to
    	            // be dropped in the output
    	            if (s == null)
    	                continue;
    	            try{
    	            	doStatement(s);
    	            }catch(RuntimeException e){
    	            	newStatements = oldStatements;
    	            	throw e;
    	            }
    	        }
    	        ArrayList<Statement> newBlockBody = new ArrayList<Statement>(newStatements.size());
    	        for(int i=newStatements.size()-1; i>=0; --i){
    	        	newBlockBody.add(newStatements.get(i));
    	        }

    	        Statement result = new StmtBlock(stmt, newBlockBody);
    	        newStatements = oldStatements;
    	        rs = result;
    	    }
    	}finally{
    		if( rs == null){
    			rs = stmt;
    		}
    		state.popLevel();
    	}
        return rs;
    }


    public Object visitStmtFork(StmtFork loop){

    	state.pushParallelSection();
    	Statement nbody = null;
        StmtVarDecl ndecl = null;
        Expression niter = null;
    	try{
	    	state.pushLevel();
	    	varDecl(loop.getLoopVarDecl());

	        try{
	        	nbody = (Statement)loop.getBody().accept(this);
	        	abstractValue viter = (abstractValue) loop.getIter().accept(this);
	        	niter = exprRV;
	        	ndecl = (StmtVarDecl) loop.getLoopVarDecl().accept(this);
	        	if(ndecl == null){
	        		ndecl = loop.getLoopVarDecl();
	        	}
	        }finally{
	    		state.popLevel();
	    	}

    	}finally{
    		state.popParallelSection();
    	}
        return isReplacer?  new StmtFork(loop, ndecl, niter, nbody) : loop;
	}



    public Object visitStmtFor(StmtFor stmt)
    {
    	state.pushLevel();
    	Statement ninit = null;
		Expression ncond = null;
		Statement nincr = null;
		Statement nbody = null;
    	try{

    		if (stmt.getInit() != null){
    			if( stmt.getInit() instanceof StmtVarDecl ){
    				varDecl((StmtVarDecl) stmt.getInit());
    			}
	        }
	        boolean goOn = true;
	        int iters = 0;
	        while(goOn){
	        	state.pushChangeTracker(null, false);
	        	boolean lisReplacer = isReplacer;
	        	isReplacer = false;
	        	abstractValue vcond = (abstractValue) stmt.getCond().accept(this);
	        	state.pushChangeTracker(vcond, false);
	        	if(vcond.hasIntVal() && vcond.getIntVal() == 0){
	        		isReplacer = lisReplacer;
	        		state.popChangeTracker();
	        		state.popChangeTracker();
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
	        }
	        abstractValue vcond = (abstractValue) stmt.getCond().accept(this);
	        ncond = exprRV;
	        state.pushChangeTracker(vcond, false);
	        try{
		        nbody = (Statement) stmt.getBody().accept(this);
		        if (stmt.getIncr() != null){
		        	nincr = (Statement) stmt.getIncr().accept(this);
	        	}
	        }finally{
	        	ChangeTracker ct = state.popChangeTracker();
	        	state.procChangeTrackers(ct);	        	
	        }
	        if (stmt.getInit() != null){
	            ninit = (Statement) stmt.getInit().accept(this);
	        }
    	}finally{
    		state.popLevel();
    	}
    	if(nbody == null && ninit == null && nincr == null) return null;
    	return new StmtFor(stmt, ninit, ncond, nincr, nbody);
    }

}
