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
 * Not supported yet. A simple break statement. This statement is used to exit the
 * innermost section of control flow, such as a for or while loop.
 * 
 * @deprecated
 * @author David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id$
 */
public class StmtBreak extends Statement
{
    /** Creates a new break statement. */
    public StmtBreak(FENode context)
    {
        super(context);
    }
    /** Creates a new break statement.
     * @deprecated
     */
    public StmtBreak(FEContext context)
    {
        super(context);
    }

    /** Accept a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        return v.visitStmtBreak(this);
    }

    public boolean equals(Object other)
    {
        // No state; any two break statements are equal.
        if (other instanceof StmtBreak)
            return true;
        return false;
    }

    public int hashCode()
    {
        // No state, so...
        return 17;
    }

    public String toString()
    {
        return "break";
    }
}

