package sketch.compiler.parallelEncoder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.ExprBinary;
import sketch.compiler.ast.core.exprs.ExprUnary;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssert;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtAtomicBlock;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.stmts.StmtExpr;
import sketch.compiler.ast.core.stmts.StmtFor;
import sketch.compiler.ast.core.stmts.StmtIfThen;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.ast.promela.stmts.StmtFork;

public class AtomizeConditionals extends FEReplacer {

	TempVarGen varGen;


	  public Object visitStmtFork(StmtFork loop){

	    	Statement body = (Statement) loop.getBody().accept(this);
	    	if(body == loop.getBody()){
	    		return loop;
	    	}
	    	return new StmtFork(loop, loop.getLoopVarDecl(), loop.getIter(), body);
	    }





	public AtomizeConditionals(TempVarGen varGen){
		this.varGen = varGen;
	}

	/**
	 * Accumulated conditionals.
	 */
	Stack<Expression> accumCondit = new Stack<Expression>();
	void pushCondit(Expression e, Statement context){
		String nm = varGen.nextVar();
		Expression cond;
		if(accumCondit.size() == 0){
			cond = e;
		}else{
			cond = accumCondit.peek();
			cond = new ExprBinary(cond, "&&", e);
		}
		addStatement(new StmtVarDecl(context, TypePrimitive.bittype, nm, cond ) );
		accumCondit.push( new ExprVar(e, nm) );
	}


	Statement invertCondit(){
		Expression ic = accumCondit.pop();
		Statement s;
		if(accumCondit.size() == 0){
			s = new StmtAssign(ic,
					 new ExprUnary(ic, ExprUnary.UNOP_NOT, ic));

		}else{
			Expression t = accumCondit.peek();
			s = new StmtAssign(ic,
					new ExprBinary(t, "&&", new ExprUnary(ic, ExprUnary.UNOP_NOT, ic)));
		}
		accumCondit.push(ic);
		return s;
	}


	void popCondit(){
		if(accumCondit.size() == 0){
			System.out.print("I found it");
		}
		accumCondit.pop();
	}

	public Statement fixStmt(Statement stmt){
		if(accumCondit.size() == 0){
			return stmt;
		}else{
			return new StmtIfThen(stmt, accumCondit.peek(), stmt, null);
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
	                post.add((Statement)new StmtAssign(new ExprVar(oinit, stmt.getName(i)), oinit).accept(this));
	            }
	            newInits.add(null);
	        }
	        if(!changed){ return stmt; }
	        this.addStatement( new StmtVarDecl(stmt, stmt.getTypes(),
                    stmt.getNames(), newInits) );
	        return new StmtBlock(stmt, post);
	    }


	 public Object visitStmtFor(StmtFor stmt)
	    {


	        Statement newBody = (Statement)stmt.getBody().accept(this);
	        if (newBody == stmt.getBody())
	            return stmt;
	        return new StmtFor(stmt, stmt.getInit(), stmt.getCond(), stmt.getIncr(),
 newBody,
                stmt.isCanonical());
	    }


	 public boolean hasCondAtomic(Statement stmt){
		 
		 class HasCA extends FEReplacer{
			 public boolean has = false;
			 @Override
			 public Object visitStmtAtomicBlock(StmtAtomicBlock ab){
				 if(ab.isCond()){ has = true; }
				 return ab;
			 }
			 public Object visitStmtAssign(StmtAssign sa){
				 return sa;
			 }
		 }
		 HasCA h = new HasCA();
		 stmt.accept(h);
		 return h.has;
	 }
	 
	 
	 public Object visitStmtIfThen(StmtIfThen stmt)
	    {

	    	if( isSingleStmt(stmt.getCons()) ){
	    		Statement rv = null;
	    		if(stmt.getAlt() == null){
	    			rv= fixStmt(stmt);
	    		}else if(isSingleStmt(stmt.getAlt()) ){
	    			rv = fixStmt(stmt);
	    		}
	    		if(rv != null){
	    			if( hasCondAtomic(rv) ){
	    				return rv;
	    			}else{
	    				return new StmtAtomicBlock(stmt, Collections.singletonList(rv) );
	    			}
	    		}
	    	}

	    	pushCondit(stmt.getCond(), stmt);
	    	Statement s1 = (Statement) stmt.getCons().accept(this);

	    	if(stmt.getAlt() != null){
	    		Statement si = invertCondit();
	    		Statement s2 = (Statement) stmt.getAlt().accept(this);
	    		popCondit();
	    		s1 = new StmtBlock(s1, new StmtBlock(si, s2));
	    	}else{
	    		popCondit();

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
