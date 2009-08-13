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

package sketch.compiler.passes.streamit_old;
import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEVisitor;
import sketch.compiler.ast.core.SplitterJoiner;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.Expression;

/**
 * A fixed-weight round-robin splitter or joiner.  This has a single
 * expression, which is the number of items to take or give to each
 * tape.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id$
 */
public class SJRoundRobin extends SplitterJoiner
{
    private Expression weight;

    /** Creates a new round-robin splitter or joiner with the specified
     * weight. */
    public SJRoundRobin(FENode context, Expression weight)
    {
        super(context);
        this.weight = weight;
    }

    /** Creates a new round-robin splitter or joiner with the specified
     * weight.
     * @deprecated
     */
    public SJRoundRobin(FEContext context, Expression weight)
    {
        super(context);
        this.weight = weight;
    }

    /** Creates a new round-robin splitter or joiner with weight 1. */
    public SJRoundRobin(FENode context)
    {
        this(context, new ExprConstInt(context, 1));
    }

    /**
     * Creates a new round-robin splitter or joiner with weight 1.
     * @deprecated
     */
    public SJRoundRobin(FEContext context)
    {
        this(context, new ExprConstInt(context, 1));
    }

    /** Returns the number of items distributed to or from each tape. */
    public Expression getWeight()
    {
        return weight;
    }

    /** Accept a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        return v.visitSJRoundRobin(this);
    }
}
