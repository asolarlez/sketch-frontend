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

package sketch.compiler.ast.core.stmts;
import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEVisitor;

/**
 * A loop that executes its body a specified number of times.
 */
public class StmtMinLoop extends Statement
{
    private Statement body;

    /** Creates a new loop. */
    public StmtMinLoop(FENode context, Statement body)
    {
        super(context);
        this.body = body;
    }

    /** Creates a new loop.
     * @deprecated
     */
    public StmtMinLoop(FEContext context, Statement body)
    {
        super(context);
        this.body = body;
    }

    /** Return the loop body of this. */
    public Statement getBody()
    {
        return body;
    }

    /** Accept a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        return v.visitStmtMinLoop(this);
    }

    public String toString()
    {
    	return "minloop";
    }
}


