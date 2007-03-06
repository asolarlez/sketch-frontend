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

import java.util.*;

/**
 * A call to a particular named function.  This contains the name of
 * the function and a <code>java.util.List</code> of parameters.  Like
 * other <code>Expression</code>s, this is immutable; an unmodifiable
 * copy of the passed-in <code>List</code> is saved.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id$
 */
public class ExprFunCall extends Expression
{
    private final String name;
    private final List<Expression> params;
    
    /** Creates a new function call with the specified name and
     * parameter list. */
    public ExprFunCall(FEContext context, String name, List params)
    {
        super(context);
        this.name = name;
        this.params = Collections.unmodifiableList(params);
    }

    /** Creates a new function call with the specified name and
     * specified single parameter. */
    public ExprFunCall(FEContext context, String name, Expression param)
    {
        super(context);
        this.name = name;
        List tmp = Collections.singletonList(param);
        this.params = Collections.unmodifiableList(tmp);
    }

    /** Creates a new function call with the specified name and
     * two specified parameters. */
    public ExprFunCall(FEContext context, String name,
                       Expression p1, Expression p2)
    {
        super(context);
        this.name = name;
        List tmp = Arrays.asList(new Object[] {p1,p2});
        this.params = Collections.unmodifiableList(tmp);
    }

    /** Returns the name of the function being called. */
    public String getName()
    {
        return name;
    }
    
    /** Returns the parameters of the function call, as an unmodifiable
     * list. */
    public List<Expression> getParams()
    {
        return params;
    }
    
    /** Accept a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        return v.visitExprFunCall(this);
    }
    
    public String toString()
    {
    	return name+"("+params+")";
    }
}
