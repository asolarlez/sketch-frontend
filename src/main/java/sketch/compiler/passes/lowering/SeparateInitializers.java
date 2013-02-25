/*
 * Copyright 2003 by the Massachusetts Institute of Technology.
 *
 * Permission to use, copy, modify, and distribute this
 * software and its documentation for any purpose and without
 * fee is hereby granted, provided that the above copyright
 * notice appear in all copies and that both that copyright
 * notice and this permission notice appear in supporting
 * documentation, and that the name of M.I.T. not be used in
 * advertising or publicity pertaining to distribution of the
 * software without specific, written prior permission.
 * M.I.T. makes no representations about the suitability of
 * this software for any purpose.  It is provided "as is"
 * without express or implied warranty.
 */

package sketch.compiler.passes.lowering;
import java.util.ArrayList;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtFor;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.promela.stmts.StmtFork;

/**
 * Separate variable initializers into separate statements.  Given
 * initialized variables like
 *
 * <pre>
 * int c = 4;
 * </pre>
 *
 * separate this into two statements like
 *
 * <pre>
 * int c;
 * c = 4;
 * </pre>
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id$
 */
public class SeparateInitializers extends FEReplacer
{
    public Object visitStmtVarDecl(StmtVarDecl stmt)
    {
        // Make sure the variable declaration stays first.  This will
        // have no initializers, except for where there is an array
        // initializer.
	ArrayList newInits = new ArrayList(stmt.getNumVars());
	for (int i=0; i<stmt.getNumVars(); i++) {
	    //if (stmt.getInit(i) instanceof ExprArrayInit) {
		//newInits.add(stmt.getInit(i));
	    //} else {
		newInits.add(null);
	    //}
	}
        Statement newDecl = new StmtVarDecl(stmt,
                                            stmt.getTypes(),
                                            stmt.getNames(),
                                            newInits);
        addStatement(newDecl);

        // Now go through the original statement; if there are
        // any initializers, create a new assignment statement.
        for (int i = 0; i < stmt.getNumVars(); i++)
        {
            String name = stmt.getName(i);
            Expression init = stmt.getInit(i);
	    // don't separate array initializations, because it become
	    // illegal syntax
            if (init != null) //&& !(init instanceof ExprArrayInit))
            {
                Statement assign = new StmtAssign(new ExprVar(stmt, name), init);
                addStatement(assign);
            }
        }

        // Already added the base statement.
        return null;
    }


    public Object visitStmtFork(StmtFork loop){
//    	 Only recurse into the body.
    	StmtVarDecl decl = loop.getLoopVarDecl();
    	Expression niter = loop.getIter();
    	Statement body = (Statement) loop.getBody().accept(this);
    	if(decl == loop.getLoopVarDecl() && niter == loop.getIter() && body == loop.getBody()  ){
    		return loop;
    	}
    	return new StmtFork(loop, decl, niter, body);
    }

    public Object visitStmtFor(StmtFor stmt)
    {
        // Only recurse into the body.
        Statement newBody = (Statement)stmt.getBody().accept(this);
        if (newBody == stmt.getBody())
            return stmt;
        return new StmtFor(stmt, stmt.getInit(), stmt.getCond(), stmt.getIncr(), newBody,
                stmt.isCanonical());
    }
}
