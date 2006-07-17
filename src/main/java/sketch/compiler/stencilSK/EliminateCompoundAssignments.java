package streamit.frontend.stencilSK;

import streamit.frontend.nodes.ExprBinary;
import streamit.frontend.nodes.Expression;
import streamit.frontend.nodes.FEReplacer;
import streamit.frontend.nodes.StmtAssign;

public class EliminateCompoundAssignments extends FEReplacer {

	public EliminateCompoundAssignments() {
		super();
		// TODO Auto-generated constructor stub
	}
	

	 public Object visitStmtAssign(StmtAssign stmt)
	    {
	        Expression newLHS = doExpression(stmt.getLHS());
	        Expression newRHS = doExpression(stmt.getRHS());
	        if( stmt.getOp() != 0){
	        	newRHS = new ExprBinary(stmt.getContext(), stmt.getOp(), newLHS, newRHS);
	        }
	        if (newLHS == stmt.getLHS() && newRHS == stmt.getRHS())
	            return stmt;
	        return new StmtAssign(stmt.getContext(), newLHS, newRHS,
	                              stmt.getOp());
	    }
}
