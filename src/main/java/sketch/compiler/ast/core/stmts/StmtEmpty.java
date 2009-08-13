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
 * An empty statement.  This does nothing; it is mostly useful as a
 * placeholder in things like for loops.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id$
 */
public class StmtEmpty extends Statement
{
	public static final StmtEmpty EMPTY = new StmtEmpty((FENode)null);
    /**
     * Creates a new empty statement.
     *
     * @param context   File and line number the statement corresponds to
     */
    public StmtEmpty(FENode context)
    {
        super(context);
    }

    /**
     * Creates a new empty statement.
     *
     * @param context   File and line number the statement corresponds to
     * @deprecated
     */
    public StmtEmpty(FEContext context)
    {
        super(context);
    }

    public Object accept(FEVisitor v)
    {
        return v.visitStmtEmpty(this);
    }
}
