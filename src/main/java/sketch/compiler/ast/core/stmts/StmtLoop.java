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
import sketch.compiler.ast.core.exprs.Expression;

/**
 * A loop that executes its body a specified number of times, the "repeat(n) stmt"
 * construct, where "n" must be a compile-time constant, but that constant can be some
 * expression with unknown choices like a hole "??". Note: should be renamed to
 * "StmtRepeat".
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

    @Override
    public int size() {
        return body == null ? 0 : body.size();
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
        return "repeat(" + iter + "){\n" + this.body + "\n}";
    }
}


