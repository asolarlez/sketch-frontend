package sketch.compiler.spin;

import java.util.HashSet;
import java.util.List;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.exprs.ExprArrayRange;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.exprs.ExprArrayRange.Range;
import sketch.compiler.ast.core.exprs.ExprArrayRange.RangeLen;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.Type;



/**
 *
 * Assumptions. For this pass, we are assuming that all variable
 * names in the program are unique. This is enforced by the preprocessing pass.
 *
 *
 * @author asolar
 *
 */


public class IdentifyModifiedVars extends FEReplacer {
	boolean isLeft = false;
	public HashSet<String> lhsVars = new HashSet<String>();
	public HashSet<String> rhsVars = new HashSet<String>();
	public HashSet<String> locals = new HashSet<String>();

	public Object visitStmtAssign(StmtAssign stmt)
    {
	 	boolean tmpLeft = isLeft;
	 	isLeft = true;
        Expression newLHS = doExpression(stmt.getLHS());
        isLeft = tmpLeft;
        Expression newRHS = doExpression(stmt.getRHS());
        return stmt;
    }


	public Object visitExprArrayRange(ExprArrayRange exp) {
		boolean tmpLeft = isLeft;

		// This is weird, but arrays can't be parameters to functions in
		// Promela.  So we'll be conservative and always treat them as
		// LHS expressions.
		isLeft = true;
		doExpression(exp.getBase());
		isLeft = tmpLeft;

		final List l=exp.getMembers();
		for(int i=0;i<l.size();i++) {
			Object obj=l.get(i);
			if(obj instanceof Range) {
				Range range=(Range) obj;
				tmpLeft = isLeft;
			 	isLeft = false;
				doExpression(range.start());
				doExpression(range.end());
				isLeft = tmpLeft;
			}
			else if(obj instanceof RangeLen) {
				RangeLen range=(RangeLen) obj;
				tmpLeft = isLeft;
			 	isLeft = false;
				doExpression(range.start());
				isLeft = tmpLeft;
			}
		}
		return exp;
	}

	public Object visitStmtVarDecl(StmtVarDecl stmt)
    {
        for (int i = 0; i < stmt.getNumVars(); i++)
        {
            Expression init = stmt.getInit(i);
            if (init != null)
                init = doExpression(init);
            Type t = (Type) stmt.getType(i).accept(this);
            locals.add(stmt.getName(i));
        }
        return stmt;
    }

	public Object visitExprVar(ExprVar exp) {
		if(locals.contains(exp.getName()))
			return exp;
		if(isLeft){
			lhsVars.add(exp.getName());
		}else{
			rhsVars.add(exp.getName());
		}
		 return exp;
	}
}