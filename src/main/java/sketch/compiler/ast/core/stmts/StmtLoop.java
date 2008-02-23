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

package streamit.frontend.nodes;

/**
 * A loop that executes its body a specified number of times.
 */
public class StmtLoop extends Statement
{
    private Expression iter;
    private Statement body;

    /** Creates a new loop. */
    public StmtLoop(FENode context, Expression iter, Statement body)
    {
        super(context);
        this.iter = iter;
        this.body = body;
    }

    /** Creates a new loop.
     * @deprecated
     */
    public StmtLoop(FEContext context, Expression iter, Statement body)
    {
        super(context);
        this.iter = iter;
        this.body = body;
    }

    /** Return the number of iterations. */
    public Expression getIter()
    {
        return iter;
    }

    /** Return the loop body of this. */
    public Statement getBody()
    {
        return body;
    }

    /** Accept a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        return v.visitStmtLoop(this);
    }

    public String toString()
    {
    	return "loop("+iter+")";
    }
}


