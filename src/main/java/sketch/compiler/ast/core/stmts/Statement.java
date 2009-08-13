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
 * A generic statement, as created in the front-end.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id$
 */
abstract public class Statement extends FENode
{
    public Statement(FENode context)
    {
        super(context);
    }

    /**
     *
     * @param context
     * @deprecated
     */
    public Statement(FEContext context)
    {
        super(context);
    }

    public boolean isBlock () { return false; }
    
    public Statement doStatement(FEVisitor visit){
    	return (Statement) this.accept(visit);
    }
}
