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
 * An array-element reference.  This is an expression like
 * <code>a[n]</code>.  There is a base expression (the "a") and an
 * offset expresion (the "n").
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id$
 * 
 */
public interface ExprArray 
{    
    /** Creates a new ExprArray with the specified base and offset. */
    /** Returns the base expression of this. */
    public Expression getBase();

    /** Returns the offset expression of this. */
    public Expression getOffset();
    
    /** Accept a front-end visitor. */
    public Object accept(FEVisitor v);

    /**
     * Determine if this expression can be assigned to.  Array
     * elements can always be assigned to.
     *
     * @return always true
     */
    public boolean isLValue();

    public int hashCode();
    
    public boolean equals(Object o);

	/**
	 * @param unchecked The unchecked to set.
	 */
	public void setUnchecked(boolean unchecked);
	/**
	 * @return Returns the unchecked.
	 */
	public boolean isUnchecked();
}
