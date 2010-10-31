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

package sketch.compiler.ast.core.typs;
import static java.util.Collections.unmodifiableList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import sketch.compiler.ast.core.FEVisitor;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.cuda.typs.CudaMemoryType;

/**
 * A fixed-length homogenous array type.  This type has a base type and
 * an expression for the length.  The expression must be real and integral,
 * but may contain variables if they can be resolved by constant propagation.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id$
 */
public class TypeArray extends Type
{
    private Type base;
    private Expression length;
    private List<Expression> dims;

    /**
     * Creates an array type of the specified base type with the specified length.
     */
    public TypeArray(Type base, Expression length) {
        this(CudaMemoryType.UNDEFINED, base, length, null);
    }

    /**
     * Create an array with the given base type, length, and dimensions. It is assumed,
     * but not checked, that \product{dims} = length.
     * 
     * @param base
     *            base type of the array
     * @param length
     *            number of elements of the base type
     * @param dims
     *            the "virtual dimensions" of the array
     */
    public TypeArray(Type base, Expression length, Collection<Expression> dims) {
        this(CudaMemoryType.UNDEFINED, base, length, dims);
    }

    public TypeArray(CudaMemoryType cuda_mem_typ, Type base, Expression length,
            Collection<Expression> dims)
    {
        super(cuda_mem_typ);
        this.base = base;
        this.length = length;
        if (dims != null) {
            this.dims = unmodifiableList(new ArrayList<Expression>(dims));
        }
    }

    public TypeArray(CudaMemoryType mem_typ, Type type, int i) {
        this(mem_typ, type, new ExprConstInt(i), null);
    }

    public boolean isArray () { return true; }

    /** Gets the base type of this. */
    public Type getBase()
    {
        return base;
    }

    /** Gets the length of this. */
    public Expression getLength()
    {
        return length;
    }

    public Expression defaultValue () {
    	return getBase ().defaultValue ();
    }

    public String toString()
    {
        return this.getCudaMemType().syntaxNameSpace() + this.getBase() + "[" + length + "]";
    }

    public boolean promotesTo(Type other)
    {
        if (super.promotesTo(other))
            return true;
        if (!(other instanceof TypeArray))
            return false;
        TypeArray that = (TypeArray)other;
        if (!(this.getBase().promotesTo(that.getBase())))
            return false;
        Expression thisLen = this.getLength();
        Expression thatLen = that.getLength();
        if(thisLen.getIValue() != null && thatLen.getIValue() != null){
        	int ithis = thisLen.getIValue();
        	int ithat = thatLen.getIValue();
        	return ithis <= ithat;
        }
//        if (!(thisLen.equals(thatLen)))
//            return false;
        return true;
    }


    public boolean equals(Object other)
    {
        if (!(other instanceof TypeArray))
            return false;
        TypeArray that = (TypeArray)other;
        if (!(this.getBase().equals(that.getBase())))
            return false;
        Expression thisLen = this.getLength();
        Expression thatLen = that.getLength();
        if(thisLen.getIValue() != null && thatLen.getIValue() != null){
        	return thisLen.getIValue().equals(thatLen.getIValue());
        }
        if (!(thisLen.equals(thatLen)))
            return false;
        return true;
    }

    public int hashCode()
    {
        return base.hashCode() ^ length.hashCode();
    }

    public List<Expression> getDimensions() {
    	// XXX/cgjones: shortcut for flattened multi-dimension arrays
    	if (null != dims)
    		return new ArrayList<Expression> (dims);

    	List<Expression> ret=new ArrayList<Expression>();
    	Type t=this;
    	while(t instanceof TypeArray) {
    		TypeArray ta=(TypeArray) t;
    		ret.add(0,ta.getLength());
    		t=ta.getBase();
    	}
    	return ret;
    }

    /** Get the i'th dimension of this array.  Counts from 0. */
    public Expression getDimension (int i) {
    	return getDimensions ().get (i);
    }

    public Object accept(FEVisitor visitor){
    	return visitor.visitTypeArray(this);
    }

    @Override
    public Type withMemType(CudaMemoryType memtyp) {
        return new TypeArray(memtyp, base, length, dims);
    }
}
