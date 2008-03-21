package streamit.frontend.spin;

import java.util.HashSet;
import java.util.List;

import streamit.frontend.nodes.ExprArrayRange;
import streamit.frontend.nodes.ExprVar;
import streamit.frontend.nodes.Expression;
import streamit.frontend.nodes.FEReplacer;
import streamit.frontend.nodes.StmtAssign;
import streamit.frontend.nodes.StmtVarDecl;
import streamit.frontend.nodes.Type;
import streamit.frontend.nodes.ExprArrayRange.Range;
import streamit.frontend.nodes.ExprArrayRange.RangeLen;



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