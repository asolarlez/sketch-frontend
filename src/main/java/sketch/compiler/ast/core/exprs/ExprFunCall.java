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
import static sketch.util.Misc.nonnull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEVisitor;
import sketch.compiler.ast.core.typs.Type;

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
	private final String name_var;
    private static int NEXT_UID=0;

    private int clusterId; // Used to identify the cluster to combine funCalls.
    private int callid;
    private final List<Expression> params;
    private final Map<String, Type> tparams;
    
	public static class ExprFunCallCreator {
		FENode context;
	    private String name;
		private String name_var;

	    private List<Expression> params;
	    private Map<String, Type> tparams;

	    ExprFunCallCreator(ExprFunCall source) {
			context = source;
			this.name = source.name; // no need to clone
			this.name_var = source.name_var;
			this.params = new ArrayList<Expression>(source.params);
			if (source.tparams == null) {
				this.tparams = null;
			} else {
				this.tparams = new HashMap<String, Type>(source.tparams);
			}
		}

		public ExprFunCallCreator name(String new_name) {
			if (this.name_var == "") {
				this.name_var = name;
			}
			name = new_name;
			return this;
		}

		public ExprFunCall create() {
			return new ExprFunCall(context, name, name_var, params, tparams);
		}
	}

	public ExprFunCallCreator creator() {
		return new ExprFunCallCreator(this);
	}

    public void resetCallid(){
    	this.callid = NEXT_UID++;
    }

    public int getCallid(){
    	return callid;
    }
    
    /** Creates a new function call with the specified name and
     * parameter list. */
    public ExprFunCall(FENode context, String name, List<Expression> params, Map<String, Type> tparams)
    {
        super(context);
        this.name = nonnull(name, "Cannot construct a function call without a name!");
		this.name_var = "";
        this.params = Collections.unmodifiableList(params);
        if(context instanceof ExprFunCall){
        	this.callid = ((ExprFunCall)context).callid;
            this.clusterId = ((ExprFunCall) context).getClusterId();
        }else{
        	this.callid = NEXT_UID++;
        }
        this.tparams = tparams;
    }

	public ExprFunCall(FENode context, String name, String name_var, List<Expression> params,
			Map<String, Type> tparams) {
		super(context);
		this.name = nonnull(name, "Cannot construct a function call without a name!");
		this.name_var = name_var;
		this.params = Collections.unmodifiableList(params);
		if (context instanceof ExprFunCall) {
			this.callid = ((ExprFunCall) context).callid;
			this.clusterId = ((ExprFunCall) context).getClusterId();
		} else {
			this.callid = NEXT_UID++;
		}
		this.tparams = tparams;
	}

    /**
     * Creates a new function call with the specified name and parameter list.
     */
    public ExprFunCall(FENode context, String name, List<Expression> params) {
        this(context, name, params, null);
    }


    /** Creates a new function call with the specified name and
     * parameter list.
     * @deprecated
     */
    public ExprFunCall(FEContext context, String name, List<Expression> params)
    {
        super(context);
        this.name = nonnull(name, "Cannot construct a function call without a name!");
		this.name_var = "";
        this.params = Collections.unmodifiableList(params);
        this.callid = NEXT_UID++;
        tparams = null;
    }

    /** Creates a new function call with the specified name and
     * specified single parameter. */
    public ExprFunCall(FENode context, String name, Expression param)
    {
        this(context, name, Collections.singletonList(param), null);
    }

    /**
     * Creates a new function call with the specified name and two specified parameters.
     */
    public ExprFunCall(FENode context, String name, Expression... params) {
        this(context, name, Arrays.asList(params), null);
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

    /**
     * 
     * @return Returns type parameters to the function. They could be null.
     */
    public Map<String, Type> getTypeParams() {
        return tparams;
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
    
    String printTParams() {
        if (tparams == null) {
            return "";
        }
        return "//" + tparams.toString();
    }

    public String toString()
    {
        return name + "(" + printParams() + ")" + printTParams();
    }

	public String get_name_var() {
		if (name_var == "") {
			return getName();
		} else {
			return name_var;
		}
	}
}
