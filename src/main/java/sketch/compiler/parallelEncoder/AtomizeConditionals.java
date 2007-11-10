package streamit.frontend.parallelEncoder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import streamit.frontend.nodes.ExprBinary;
import streamit.frontend.nodes.ExprUnary;
import streamit.frontend.nodes.ExprVar;
import streamit.frontend.nodes.Expression;
import streamit.frontend.nodes.FEReplacer;
import streamit.frontend.nodes.Statement;
import streamit.frontend.nodes.StmtAssert;
import streamit.frontend.nodes.StmtAssign;
import streamit.frontend.nodes.StmtAtomicBlock;
import streamit.frontend.nodes.StmtBlock;
import streamit.frontend.nodes.StmtExpr;
import streamit.frontend.nodes.StmtFor;
import streamit.frontend.nodes.StmtIfThen;
import streamit.frontend.nodes.StmtPloop;
import streamit.frontend.nodes.StmtVarDecl;
import streamit.frontend.nodes.TempVarGen;
import streamit.frontend.nodes.TypePrimitive;

public class AtomizeConditionals extends FEReplacer {

	TempVarGen varGen;
	
	
	  public Object visitStmtPloop(StmtPloop loop){	    	
	    	
	    	Statement body = (Statement) loop.getBody().accept(this);
	    	if(body == loop.getBody()){
	    		return loop;
	    	}
	    	return new StmtPloop(loop.getCx(), loop.getLoopVarDecl(), loop.getIter(), body);
	    }
	
	
	
	
	
	public AtomizeConditionals(TempVarGen varGen){
		this.varGen = varGen;
	}
	
	/**
	 * Accumulated conditionals.
	 */
	Stack<Expression> accumCondit = new Stack<Expression>();
	Set<String> freeVars = new HashSet<String>();
	List<Statement> decls = new ArrayList<Statement>();
	ExprVar getVar(){
		String nm;
		if(freeVars.size() > 0){
			nm = freeVars.iterator().next();
			freeVars.remove(nm);
		}else{
			nm = varGen.nextVar();
			decls.add(new StmtVarDecl(null, TypePrimitive.bittype, nm, null ));
		}
		return new ExprVar(null, nm);
		
	}
	
	boolean firstTime = true;
	
	public Object visitStmtBlock(StmtBlock stmt)
    {
		Object rv ;
		if(firstTime){		
			firstTime = false;
			rv = super.visitStmtBlock(stmt);			
			decls.add((Statement)rv);
			rv = new StmtBlock(stmt.getCx(), decls);
			decls = new ArrayList<Statement>();
			firstTime = true;			
		}else{
			rv = super.visitStmtBlock(stmt);
		}
		return rv;		
    }
	
	void pushCondit(Expression e){
		Expression cond;
		if(accumCondit.size() == 0){
			cond = e;
		}else{
			cond = accumCondit.peek();
			cond = new ExprBinary(cond, "&&", e);			
		}
		ExprVar v = getVar();		
		addStatement(new StmtAssign(e.getCx(), v, cond ) );
		accumCondit.push( v );
	}
	
	void popCondit(){
		Expression e = accumCondit.pop();
		freeVars.add(((ExprVar)e).getName());
	}
	
	public Statement fixStmt(Statement stmt){
		if(accumCondit.size() == 0){
			return stmt;
		}else{
			return new StmtIfThen(stmt.getCx(), accumCondit.peek(), stmt, null);
		}
	}
	
	@Override
	public Object visitStmtAssign(StmtAssign stmt){
		return fixStmt(stmt);
    }
	
	@Override
	public Object visitStmtAssert(StmtAssert stmt){
		 return fixStmt(stmt);
    }
	
	 @Override
	 public Object visitStmtAtomicBlock (StmtAtomicBlock stmt) {
		 return fixStmt(stmt);
	 }
	 @Override
	 public Object visitStmtExpr (StmtExpr stmt) {
		 return fixStmt(stmt);
	 }
	 
	 
	 
	 
	 public Object visitStmtVarDecl(StmtVarDecl stmt)
	    {
		 	if(accumCondit.size() == 0) return stmt;
	        List<Expression> newInits = new ArrayList<Expression>();	        
	        List<Statement> post = new ArrayList<Statement>();
	        boolean changed = false;
	        for (int i = 0; i < stmt.getNumVars(); i++)
	        {
	            Expression oinit = stmt.getInit(i);
	            if (oinit != null){
	            	changed = true;
	                post.add((Statement)new StmtAssign(null, new ExprVar(null, stmt.getName(i)), oinit).accept(this));
	            }
	            newInits.add(null);
	        }
	        if(!changed){ return stmt; }
	        this.addStatement( new StmtVarDecl(stmt.getContext(), stmt.getTypes(),
                    stmt.getNames(), newInits) );	        
	        return new StmtBlock(stmt.getCx(), post);
	    }
	 
	 
	 public Object visitStmtFor(StmtFor stmt)
	    {

	        
	        Statement newBody = (Statement)stmt.getBody().accept(this);
	        if (newBody == stmt.getBody())
	            return stmt;
	        return new StmtFor(stmt.getContext(), stmt.getInit(), stmt.getCond(), stmt.getIncr(),
	                           newBody);
	    }
	
	 
	 public Object visitStmtIfThen(StmtIfThen stmt)
	    {
	    	
	    	if( isSingleStmt(stmt.getCons()) ){
	    		if(stmt.getAlt() == null){
	    			return stmt;
	    		}
	    		if(isSingleStmt(stmt.getAlt()) ){
	    			return stmt;
	    		}
	    	}
	    	
	    	pushCondit(stmt.getCond());
	    	Statement s1 = (Statement) stmt.getCons().accept(this);
	    	popCondit();
	    	if(stmt.getAlt() != null){
	    		pushCondit(new ExprUnary(stmt.getCx(), ExprUnary.UNOP_NOT, stmt.getCond()));
	    		Statement s2 = (Statement) stmt.getAlt().accept(this);
	    		popCondit();
	    		s1 = new StmtBlock(s1, s2);
	    	}
	    	return s1;
	    }
	 
	
	boolean isSingleStmt(Statement s){
	
		if(s instanceof StmtAssign) return true;
		if(s instanceof StmtAtomicBlock){
			return true;
		}
		if(s instanceof StmtBlock){
			StmtBlock sb = (StmtBlock) s;
			if(sb.getStmts().size() != 1){ return false; }
			return isSingleStmt(sb.getStmts().get(0));
		}
		if(s instanceof StmtExpr){
			return true;
		}		
		return false;
	}
	
	
}
