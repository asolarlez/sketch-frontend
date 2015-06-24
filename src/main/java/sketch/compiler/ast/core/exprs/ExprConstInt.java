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

package sketch.compiler.ast.core.exprs;
import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEVisitor;

/**
 * An integer-valued constant. This can be freely promoted to an ExprConstFloat when fixed
 * point representation is used for float.
 * 
 * @author David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id$
 */
public class ExprConstInt extends ExprConstant
{
    private final int val;

    public static final ExprConstInt zero = new ExprConstInt(0);
    public static final ExprConstInt one = new ExprConstInt(1);
    public static final ExprConstInt minusone = new ExprConstInt(-1);
    
    public static ExprConstInt createConstant(int i){
        if(i == 0){ return zero; }
        if(i == -1){ return minusone; }
        if(i == 1){ return one; }
        return new ExprConstInt((FENode)null, i);
    }

    /** Create a new ExprConstInt with a specified value. */
    public ExprConstInt(FENode context, int val)
    {
        super(context);
        this.val = val;
    }

    /**
     * Create a new ExprConstInt with a specified value.
     * @deprecated
     */
    public ExprConstInt(FEContext context, int val)
    {
        super(context);
        this.val = val;
    }

    /** Create a new ExprConstInt with a specified value but no context. */
    public ExprConstInt(int val)
    {
        this((FEContext) null, val);
    }

    /** Parse a string as an integer, and create a new ExprConstInt
     * from the result. */
    public ExprConstInt(FENode context, String str)
    {
        this(context, Integer.parseInt(str));
    }

    /** Returns the value of this. */
    public int getVal() { return val; }

    public Integer getIValue(){
    	return new Integer(val);
    }
    /** Accept a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        return v.visitExprConstInt(this);
    }

    public boolean equals(Object other)
    {
        if (!(other instanceof ExprConstInt))
            return false;
        return val == ((ExprConstInt)other).getVal();
    }

    public int hashCode()
    {
        return new Integer(val).hashCode();
    }

    public String toString()
    {
        return Integer.toString(val);
    }
}
