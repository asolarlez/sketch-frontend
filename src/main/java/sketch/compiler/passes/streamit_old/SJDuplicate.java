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

/**
 * A duplicating splitter.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id$
 */
public class SJDuplicate extends SplitterJoiner
{
	private int type;

	public static final int DUP=0;
	public static final int XOR=1;
	public static final int AND=2;
	public static final int OR=3;

    /** Creates a new duplicating splitter. */
    public SJDuplicate(FENode context)
    {
        super(context);
        type = 0;
    }

    /** Creates a new duplicating splitter.
     * @deprecated
     */
    public SJDuplicate(FEContext context)
    {
        super(context);
        type = 0;
    }

    /**
     *
     */
    public SJDuplicate(FENode context, int type)
    {
        super(context);
        this.type = type;
    }

    /**
     * @deprecated
     */
    public SJDuplicate(FEContext context, int type)
    {
        super(context);
        this.type = type;
    }

    /** Accept a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        return v.visitSJDuplicate(this);
    }

	/**
	 * @param type The type to set.
	 */
	public void setType(int type) {
		this.type = type;
	}

	/**
	 * @return Returns the type.
	 */
	public int getType() {
		return type;
	}
}
