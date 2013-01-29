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

package sketch.compiler.ast.core;

import sketch.compiler.ast.core.typs.TypeStruct;
import sketch.util.exceptions.NotImplementedException;


/**
 * Any node in the AST, usually created by the front-end's parser. This is the root of the
 * front-end class tree. Derived classes include statement, expression, types, and
 * functions.
 * 
 * @author David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id$
 */
public abstract class FENode
{
    private FEContext context;
    private Object tag = null;
    private FENode origin = null;
    /**
     * Create a new node with the specified context.
     *
     * @param context  file and line number for the node
     */

    public FENode getOrigin(){ return origin; }
    public void resetOrigin(){ origin = null; }
    public FENode(FENode node){
    	if (null != node) {
    		context = node.context;
    		tag = node.tag;
    		if(node.origin != null){
    			origin = node.origin;
    		}else{
    			origin = node;
    		}
    	}
    }

    /**
     * Create a new node with the specified context. Note on why it's deprecated: When the
     * parser creates nodes from scratch, it uses the FENode(FEContext) constructor to
     * create a brand new node with the context information about where in the input file
     * it came from. However, after this stage, any new FENode that is created is created
     * from another existing FENode. Keeping track of where nodes came from is important,
     * given how many transformations the AST goes through before we do the actual
     * analysis on it. For this reason, you shouldn't use the FENode(FEContext)
     * constructor when creating a node in a transformation; instead you should use the
     * FENode(FENode) constructor and pass as argument the node that this node was derived
     * from. So in other words, the FENode(FEContext) constructor is not really
     * deprecated; you need to use it when constructing nodes in the parser, but it
     * shouldn't be used anywhere else, so the deprecated status is a good way to
     * discourage its use. The same thing goes for all the constructors of the actual AST
     * nodes; all of them have a version that takes an FEContext and a version that takes
     * an FENode.
     * 
     * @param cx
     * @deprecated
     */
    public FENode (FEContext cx) {
    	context = cx;
    }

    public Object getTag(){ return tag; }
    public void setTag(Object tag){ this.tag = tag; }

    /**
	     * Returns the context associated with this node.
	     *
	     * @return context object with file and line number
	     * @deprecated Use {@link #getCx()} instead
	     *
	     */
	    public FEContext getContext()
	    {
			return getCx();
		}

	/**
     * Returns the context associated with this node.
     *
     * @return context object with file and line number
     *
     */
    public FEContext getCx()
    {
        return context;
    }

    /**
     * Calls an appropriate method in a visitor object with this as
     * a parameter.
     *
     * @param v  visitor object
     * @return   the value returned by the method corresponding to
     *           this type in the visitor object
     */
    abstract public Object accept(FEVisitor v);

    @SuppressWarnings("unchecked")
    public <T> T accept(FETypedVisitor<T> v) {
        return (T) (accept((FEVisitor) v));
    }

    @SuppressWarnings("unchecked")
    public <T> T acceptAndCast(FEVisitor v) {
        Object rv = this.accept(v);
        return (T) rv;
    }

    /**
     * Assert that COND must be true for this node.  If it is false,
     * an appropriate error is printed and a runtime exception is thrown.
     */
    public void assertTrue (boolean cond, String msg) {
    	if (!cond) {
    		report (msg);
    		throw new RuntimeException ();
    	}
    }

    /**
     * Report an error related to this AST node.
     *
     * @param errorMsg
     */
    public void report (String errorMsg) {
    	System.err.println(getCx () + ": " + errorMsg);
    }
    
    public void setCx(FEContext cx){
    	context = cx;
    }

    public static FENode anonTypeNode(TypeStruct struct) {
        return new FENode(struct.getContext()) {
            public Object accept(FEVisitor v) {
                throw new NotImplementedException(
                        "FENode anonTypeNode is just a source location reference!");
            }
        };
    }
}
