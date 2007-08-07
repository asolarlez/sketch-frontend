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

import java.util.Vector;

/**
 * An integer-valued constant.  This can be freely promoted to an
 * ExprConstFloat.  This is always real-valued, though it can appear
 * in an ExprComplex to form a complex integer expression.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id$
 */
public class ExprStar extends Expression
{		
	private int size;
	public Expression vectorSize;
	Vector<FENode> depObjects;
	private boolean isFixed;
	private Type type;
	public int INT_SIZE=5;
	
	
	public ExprStar(ExprStar old)
    {
        super(old);
        size = old.size;
        isFixed = old.isFixed;
        type = old.type;
        INT_SIZE = old.INT_SIZE;
        vectorSize = old.vectorSize;
    }
	
	public FENode getDepObject(int i){
		Type t = type;
		if(type instanceof TypePrimitive){
			return this;
		}else{
			assert type instanceof TypeArray;
			t = ((TypeArray)type).getBase();
		}
		
		if(depObjects == null){
			depObjects = new Vector<FENode>();
			depObjects.setSize(i+1);			
		}
		if(depObjects.size() <= i){
			depObjects.setSize(i+1);			
		}
		if(depObjects.get(i) == null){
			ExprStar es = new ExprStar(this);
			es.type = t;
			depObjects.set(i, new DummyFENode(getCx()));			
		}
		return depObjects.get(i);
	}
	
    /** Create a new ExprConstInt with a specified value. */
    public ExprStar(FEContext context)
    {
        super(context);
        size = 1;
        isFixed = false;
    }
    
    public ExprStar(FEContext context, int size)
    {
        super(context);
        this.size = size;
        isFixed = true;
    }

    /** Accept a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        return v.visitExprStar(this);
    }

    public boolean equals(Object other)
    {
        if (!(other instanceof ExprStar))
            return false;
        return true;
    }
    
    public String toString()
    {
    	if(getType() != null)
    		return "??:" + getType() + ":" + size;
        return "??";
    }

	/**
	 * @param size The size to set.
	 */
	public void setSize(int size) {
		this.size = size;
	}

	/**
	 * @return Returns the size.
	 */
	public int getSize() {
		return size;
	}

	/**
	 * @param isFixed The isFixed to set.
	 */
	public void setFixed(boolean isFixed) {
		this.isFixed = isFixed;
	}

	/**
	 * @return Returns the isFixed.
	 */
	public boolean isFixed() {
		return isFixed;
	}

	/**
	 * @param type the type to set
	 */
	public void setType(Type type) {
		this.type = type;
		
		Type tt = type;
		while(tt instanceof TypeArray){
			tt = ((TypeArray)tt).getBase();
		}
		if( (tt.equals(TypePrimitive.inttype) || tt.equals(TypePrimitive.ndinttype)) && !isFixed ){
			setSize(INT_SIZE);
		}
	}

	/**
	 * @return the type
	 */
	public Type getType() {
		return type;
	}
	
}
