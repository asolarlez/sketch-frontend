package streamit.frontend.passes;

import java.util.ArrayList;
import java.util.List;

import streamit.frontend.nodes.ExprArrayRange;
import streamit.frontend.nodes.ExprBinary;
import streamit.frontend.nodes.ExprConstInt;
import streamit.frontend.nodes.ExprFunCall;
import streamit.frontend.nodes.ExprVar;
import streamit.frontend.nodes.Expression;
import streamit.frontend.nodes.FENode;
import streamit.frontend.nodes.FEReplacer;
import streamit.frontend.nodes.Function;
import streamit.frontend.nodes.Statement;
import streamit.frontend.nodes.StmtAssert;
import streamit.frontend.nodes.StmtAssign;
import streamit.frontend.nodes.StmtAtomicBlock;
import streamit.frontend.nodes.StmtBlock;
import streamit.frontend.nodes.StmtExpr;
import streamit.frontend.nodes.StmtFork;
import streamit.frontend.nodes.StmtVarDecl;
import streamit.frontend.nodes.TypeArray;
import streamit.frontend.nodes.TypePrimitive;

public class EliminateLockUnlock extends FEReplacer {
	
	

	public Expression loopVar = null;
	public Expression lockLen = null;
	public ExprVar locksVar = null;
	boolean hasLocks = false;
	
	@Override
	public Object visitFunction(Function fun){
		hasLocks = false;
		Function f = (Function) super.visitFunction(fun);
		if(hasLocks){
			
			StmtVarDecl svd = new StmtVarDecl(fun, new TypeArray(TypePrimitive.inttype, lockLen  ), locksVar.getName(), ExprConstInt.zero);
			StmtBlock sb = new StmtBlock(svd, f.getBody());
			
			f = new Function(f, f.getCls(), f.getName(), f.getReturnType(), f.getParams(), f.getSpecification(), sb);
			
		}
		return f;
	}
	
	@Override
	public Object visitStmtFork(StmtFork stmt){
		loopVar = new ExprVar(stmt, stmt.getLoopVarName());
		return super.visitStmtFork(stmt);
	}
	
	public EliminateLockUnlock(int lockLen, String locksVar){
		this.locksVar  =new ExprVar((FENode)null, locksVar);		
		this.lockLen = new ExprConstInt(lockLen);
	}

	 public Object visitStmtExpr(StmtExpr stmt)
	    {
		 	if(stmt.getExpression() instanceof ExprFunCall){
		 		Object o = stmt.getExpression().accept(this);
		 		if(o instanceof Expression){
		 			if( o == null) return null;
			        if (o == stmt.getExpression()) return stmt;
			        return new StmtExpr(stmt, (Expression)o);
		 		}else{
		 			assert o instanceof Statement;
		 			return o;
		 		}
		 	}

	        Expression newExpr = doExpression(stmt.getExpression());
	        if( newExpr == null) return null;
	        if (newExpr == stmt.getExpression()) return stmt;
	        return new StmtExpr(stmt, newExpr);
	    }


	 public Object visitExprFunCall(ExprFunCall exp)
	    {


		 if(exp.getName().equals("lock")){
			 hasLocks = true;
			 assert exp.getParams().size() == 1;
			 Expression p = exp.getParams().get(0);
			 
/** This is the code we are producing here.
 * 
 *  atomic(locks[i] == 0){
 *  	locks[i] = threadID  + 1;
 *  	
 *  }
 * 
*/
			 Statement ass = new StmtAssert(exp, new ExprBinary(p, "<", lockLen), "The lock expression is out of bounds.");
			 StmtAssign getLock = new StmtAssign(new ExprArrayRange(locksVar, p),  new ExprBinary(loopVar, "+", ExprConstInt.one));

			 Expression cond =new ExprBinary(new ExprArrayRange(locksVar, p), "==", ExprConstInt.zero);
			 addStatement(ass);
			 return new StmtAtomicBlock(exp, getLock , cond);			 
		 }else  if(exp.getName().equals("unlock")){
			 hasLocks = true;
			 assert exp.getParams().size() == 1;
			 Expression p = exp.getParams().get(0);
			 List<Statement> bodyL = new ArrayList<Statement>();
			 bodyL.add(new StmtAssert(exp, new ExprBinary(p, "<", lockLen), "The lock expression is out of bounds."));
			 bodyL.add(new StmtAssert(exp, new ExprBinary(new ExprArrayRange(locksVar, p), "==", new ExprBinary(loopVar, "+", ExprConstInt.one) ), "You can't release a lock you don't own"));
			 bodyL.add(new StmtAssign(new ExprArrayRange(locksVar, p), ExprConstInt.zero ));
			 return new StmtAtomicBlock(exp, bodyL);

		 }
		 return exp;
	    }

}
