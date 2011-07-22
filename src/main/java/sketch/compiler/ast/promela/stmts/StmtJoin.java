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

package sketch.compiler.ast.promela.stmts;
import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEVisitor;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;

/**
 * Declare the joiner type for a split-join or feedback loop.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id$
 */
public class StmtJoin extends Statement
{

    Expression exp;

    /**
     * Creates a new join statement with the specified joiner type.
     *
     * @param context  file and line number this statement corresponds to
     */
    public StmtJoin(FENode context, Expression e)
    {
        super(context);
        exp = e;
    }

    /**
     * Creates a new join statement with the specified joiner type.
     *
     * @param context  file and line number this statement corresponds to
     * @deprecated
     */
    public StmtJoin(FEContext context)
    {
        super(context);
    }



    /** Accepts a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        return v.visitStmtJoin(this);
    }

    public boolean equals(Object other)
    {
        if (!(other instanceof StmtJoin))
            return false;
        return true;
    }



    public String toString()
    {
        return "join " + exp;
    }
}
