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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEVisitor;
import static sketch.util.Misc.nonnull;

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
    private static int NEXT_UID=0;

    private int clusterId; // Used to identify the cluster to combine funCalls.
    private int callid;
    private final List<Expression> params;

    public void resetCallid(){
    	this.callid = NEXT_UID++;
    }

    public int getCallid(){
    	return callid;
    }
    
    /** Creates a new function call with the specified name and
     * parameter list. */
    public ExprFunCall(FENode context, String name, List<Expression> params)
    {
        super(context);
        this.name = nonnull(name, "Cannot construct a function call without a name!");
        this.params = Collections.unmodifiableList(params);
        if(context instanceof ExprFunCall){
        	this.callid = ((ExprFunCall)context).callid;
            this.clusterId = ((ExprFunCall) context).getClusterId();
        }else{
        	this.callid = NEXT_UID++;
        }
    }


    /** Creates a new function call with the specified name and
     * parameter list.
     * @deprecated
     */
    public ExprFunCall(FEContext context, String name, List<Expression> params)
    {
        super(context);
        this.name = nonnull(name, "Cannot construct a function call without a name!");
        this.params = Collections.unmodifiableList(params);
        this.callid = NEXT_UID++;
    }

    /** Creates a new function call with the specified name and
     * specified single parameter. */
    public ExprFunCall(FENode context, String name, Expression param)
    {
    	this (context, name, Collections.singletonList (param));
    }

    /**
     * Creates a new function call with the specified name and two specified parameters.
     */
    public ExprFunCall(FENode context, String name, Expression... params) {
        this(context, name, Arrays.asList(params));
    }

    /**
     * Creates a new function call with the specified name and two specified parameters.
     */
    public ExprFunCall(FEContext context, String name, Expression... params) {
        this(context, name, Arrays.asList(params));
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

    public String printParams(){
        String s = "";
        boolean notf = false;
        for(Expression p : params){
            if(notf){ s += ", "; }
            s += p.toString();
            notf = true;
        }
        return s;
    }

    public void setClusterId(int i) {
        this.clusterId = i;
    }

    public int getClusterId() {
        return this.clusterId;
    }
    
    public String toString()
    {
        return name + "(" + printParams() + ")";
    }
}
