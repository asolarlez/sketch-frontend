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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import sketch.compiler.ast.core.FEVisitor;
import sketch.compiler.ast.core.exprs.ExprBinary;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprTernary;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.cuda.typs.CudaMemoryType;
import static java.util.Collections.unmodifiableList;

/**
 * A homogenous array type with a length. This type has a base type and an expression for
 * the length. The expression must be of integer type, but may contain variables.
 * 
 * @author David Maze &lt;dmaze@cag.lcs.mit.edu&gt; modified by Armando Solar-Lezama
 * @version $Id$
 */
public class TypeArray extends Type
{
    private final Type base;
    private final Expression length;
    private final int maxlength;
    private final List<Expression> dims;

    /**
     * Creates an array type of the specified base type with the specified length.
     */
    public TypeArray(Type base, Expression length) {
        this(base, length, length != null && length.isConstant() ? length.getIValue() : 0);
    }

    public TypeArray(Type base, Expression length, int maxlength) {
        this(CudaMemoryType.UNDEFINED, base, length, null, maxlength);
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
        this(CudaMemoryType.UNDEFINED, base, length, dims,
                length.isConstant() ? length.getIValue() : 0);
    }

    public TypeArray(Type base, Expression length, Collection<Expression> dims,
            int maxlength)
    {
        this(CudaMemoryType.UNDEFINED, base, length, dims, maxlength);
    }

    public TypeArray(CudaMemoryType cuda_mem_typ, Type base, Expression length,
            Collection<Expression> dims)
    {
        this(cuda_mem_typ, base, length, dims, length.isConstant() ? length.getIValue() : 0);
    }

    public TypeArray(CudaMemoryType cuda_mem_typ, Type base, Expression length,
            Collection<Expression> dims, int maxlength)
    {
        super(cuda_mem_typ);
        this.base = base;
        this.length = length;
        this.maxlength = maxlength;
        if (dims != null) {
            this.dims = unmodifiableList(new ArrayList<Expression>(dims));
        } else {
            this.dims = null;
        }
    }

    public TypeArray(CudaMemoryType mem_typ, Type type, int i) {
        this(mem_typ, type, new ExprConstInt(i), null, i);
    }

//    public TypeArray createWithNewLength(Expression len) {
//        return new TypeArray(this.getCudaMemType(), this.getBase(), len);
//    }

    public boolean isArray () { return true; }

    /** Gets the base type of this. */
    public Type getBase()
    {
        return base;
    }
    
    /** Gets base type of multi-dim arrays */
    public Type getAbsoluteBase() {
        Type t=this;
        while(t instanceof TypeArray) {
            TypeArray ta=(TypeArray) t;
            t=ta.getBase();
        }
        return t;
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
        String s = "";
        if (length != null) {
            s = length.toString();
        }
        return this.getCudaMemType().syntaxNameSpace() + this.getBase() + "[" + s + "]";
    }

    public Type leastCommonPromotion(Type other) {
        if (!(other instanceof TypeArray))
            return super.leastCommonPromotion(other);
        TypeArray that = (TypeArray) other;
        Type nbase = this.getBase().leastCommonPromotion(that.getBase());
        Expression thisLen = this.getLength();
        Expression thatLen = that.getLength();
        if (thisLen.equals(thatLen)) {
            return new TypeArray(nbase, thisLen);
        }
        if (thisLen.getIValue() != null && thatLen.getIValue() != null) {
            int ithis = thisLen.getIValue();
            int ithat = thatLen.getIValue();
            if (ithis <= ithat) {
                return new TypeArray(nbase, thatLen);
            } else {
                return new TypeArray(nbase, thisLen);
            }
        }
        Expression l =
                new ExprTernary(thisLen, ExprTernary.TEROP_COND, new ExprBinary(thisLen,
                        "<", thatLen), thatLen, thisLen);
        return new TypeArray(nbase, l);
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


    // public boolean equals(Object other, Vector<Pair<Parameter, Parameter>> eqParams)
    public TypeComparisonResult compare(Type other)
    {
        if (!(other instanceof TypeArray))
            return TypeComparisonResult.NEQ;

        TypeArray that = (TypeArray)other;
        if (this.getBase().compare(that.getBase()) == TypeComparisonResult.NEQ) {
            return TypeComparisonResult.NEQ;
        }

        // bases match, now compare lengths
        Expression thisLen = this.getLength();
        Expression thatLen = that.getLength();
        if (thisLen.getIValue() != null && thatLen.getIValue() != null) {
            // both length expressions have integer values
            return TypeComparisonResult.knownOrNeq(thisLen.getIValue().equals(
                    thatLen.getIValue()));
        }
        return TypeComparisonResult.knownOrMaybe(thisLen.equals(thatLen));
    }

    // public boolean equals(Object other) {
    // return equals(other, new Vector<Pair<Parameter, Parameter>>());
    // }

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
        return new TypeArray(memtyp, base, length, dims, maxlength);
    }

    public TypeArray createWithLength(Expression length) {
        return new TypeArray(this.base, length, this.getMaxlength());
    }

    public int getMaxlength() {
        return maxlength;
    }
}
