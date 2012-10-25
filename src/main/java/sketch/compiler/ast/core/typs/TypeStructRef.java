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
import sketch.compiler.ast.core.FEVisitor;
import sketch.compiler.ast.core.NameResolver;
import sketch.compiler.ast.core.exprs.ExprNullPtr;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.cuda.typs.CudaMemoryType;

/**
 * A named reference to a structure type, as defined in TypeStruct.
 * This will be produced directly by the parser, but later passes
 * should replace these with TypeStructs as appropriate.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id$
 */
public class TypeStructRef extends Type
{
    private String name;

    /** Creates a new reference to a structured type. */
    public TypeStructRef(CudaMemoryType typ, String name) {
        super(typ);
        this.name = name;
    }

    public TypeStructRef addDefaultPkg(String pkg, NameResolver nres) {
        if (name.indexOf('@') >= 0) {
            return this;
        } else {
            String nname = nres.getStructName(name, pkg);
            return new TypeStructRef(nname);
        }
    }

    /** Creates a new reference to a structured type. */
    public TypeStructRef(String name) {
        this(CudaMemoryType.UNDEFINED, name);
    }

    public Object accept(FEVisitor v)
	{
		return v.visitTypeStructRef(this);
	}
    
    /** Returns the name of the referenced structure. */
    public String getName()
    {
        return name;
    }

    public boolean isStruct () { return true; }

    public Expression defaultValue () {
    	return ExprNullPtr.nullPtr;
    }

    public int hashCode()
    {
        return name.hashCode();
    }

    public String toString()
 {
        return this.getCudaMemType().syntaxNameSpace() + name;
    }
    
    @Override
    public Type withMemType(CudaMemoryType memtyp) {
        return new TypeStructRef(memtyp, name);
    }

    @Override
    public TypeComparisonResult compare(Type other) {
        if (other instanceof TypeStruct) {
            TypeStruct that = (TypeStruct) other;
            return TypeComparisonResult.knownOrNeq(name.equals(that.getName()));
        }

        if (other instanceof TypeStructRef) {
            TypeStructRef that = (TypeStructRef) other;
            return TypeComparisonResult.knownOrNeq(this.name.equals(that.name));
        }

        return TypeComparisonResult.NEQ;
    }
}
