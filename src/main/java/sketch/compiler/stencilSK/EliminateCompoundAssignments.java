package sketch.compiler.stencilSK;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.exprs.ExprBinary;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.StmtAssign;

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
	        	newRHS = new ExprBinary(stmt, stmt.getOp(), newLHS, newRHS);
	        }
	        if (newLHS == stmt.getLHS() && newRHS == stmt.getRHS())
	            return stmt;
        return new StmtAssign(stmt, newLHS, newRHS, 0);
	    }
}
