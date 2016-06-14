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

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import sketch.compiler.ast.core.FEVisitor;
import sketch.compiler.ast.core.NameResolver;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.cuda.typs.CudaMemoryType;
import sketch.util.exceptions.NotImplementedException;

/**
 * Base class for variable data types.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id$
 */
public abstract class Type
{
    protected CudaMemoryType memtyp;


    public Type(CudaMemoryType memtyp) {
        this.memtyp = memtyp;
    }

    public Type addDefaultPkg(String pkg, NameResolver nres) {
        return this;
    }


    /** @return true iff this type is a struct type. */
    public boolean isStruct () { return false; }

    /** @return true iff this type is an array type. */
    public boolean isArray () { return false; }

    public CudaMemoryType getCudaMemType() {
        return memtyp;
    }

    // ERROR! this is a huge bug! If you set a type such as "int" to global, then all int
    // becomes global and you are screwed!
    // public void setCudaMemType(CudaMemoryType mt) {
    // memtyp = mt;
    // }

    public Expression defaultValue () {
    	assert false : "Implement me!";
    	return null;	// unreachable
    }

    /**
     * Check if this type can be promoted to some other type.
     * Returns true if a value of this type can be assigned to
     * a variable of that type.
     *
     * @param that  other type to check promotion to
     * @return      true if this can be promoted to that
     */
    public boolean promotesTo(Type that, NameResolver nres)
    {
        if ((this instanceof NotYetComputedType) || (that instanceof NotYetComputedType))
        {
            return true;
        }
        if (this.equals(that))
            return true;
        return false;
    }

    public final boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Type) {
            return compare((Type) obj) == TypeComparisonResult.EQ;
        } else {
            return false;
        }
    }

    public abstract TypeComparisonResult compare(Type that);

    /**
     * Find the lowest type that two types can promote to.
     *
     * @param that  other type
     * @return      a type such that both this and that can promote
     *              to type, or null if there is no such type
     */
    public Type leastCommonPromotion(Type that, NameResolver nres)
    {
        if (this.promotesTo(that, nres))
            return that;
        if (that.promotesTo(this, nres))
            return this;
        return null;
    }

    public Object accept(FEVisitor visitor){
    	return visitor.visitType(this);
    }

    public Type withMemType(CudaMemoryType memtyp) {
        throw new NotImplementedException();
    }

    public abstract Collection<Type> getBaseTypes();

    public abstract Map<String, Type> unify(Type t, Set<String> names);

    public abstract String cleanName();

}
