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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.FEVisitor;
import sketch.compiler.ast.core.NameResolver;
import sketch.compiler.ast.core.exprs.ExprNamedParam;
import sketch.compiler.ast.core.exprs.ExprNew;
import sketch.compiler.ast.core.exprs.ExprNullPtr;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.cuda.typs.CudaMemoryType;

/**
 * A named reference to a structure type, as defined in TypeStruct. This will be produced
 * directly by the parser, but later passes might replace these with TypeStructs as
 * appropriate.
 * 
 * @author David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id$
 */
public class TypeStructRef extends Type
{
    private String name;
    private List<Type> params;
    private final boolean isUnboxed;

    /** Creates a new reference to a structured type. */
    public TypeStructRef(CudaMemoryType typ, String name, boolean isUnboxed) {
        super(typ);
        this.name = name;
        this.isUnboxed = isUnboxed;
    }

    public TypeStructRef(CudaMemoryType typ, String name, boolean isUnboxed, List<Type> params) {
        super(typ);
        this.name = name;
        this.isUnboxed = isUnboxed;
        this.params = params;
    }

    public TypeStructRef(String name, boolean isUnboxed, List<Type> params) {
        super(CudaMemoryType.UNDEFINED);
        this.name = name;
        this.isUnboxed = isUnboxed;
        this.params = params;
    }

    public void addParams(List<Type> tp) {
        params = tp;
    }

    public List<Type> getTypeParams() {
        return params;
    }

    public boolean hasTypeParams() {
        return params != null && !params.isEmpty();
    }

    public TypeStructRef addDefaultPkg(String pkg, NameResolver nres) {
        if (name.indexOf('@') >= 0) {
            return this;
        } else {
            String nname = nres.getStructName(name, pkg);
            if (nname == null) {
                nname = name;
            }
            return new TypeStructRef(nname, isUnboxed, params);
        }
    }


    /** Creates a new reference to a structured type. */
    public TypeStructRef(String name, boolean isUnboxed) {
        this(CudaMemoryType.UNDEFINED, name, isUnboxed);
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
        if (isUnboxed)
            return new ExprNew((FEContext) null, this, new ArrayList<ExprNamedParam>(), false);
    	return ExprNullPtr.nullPtr;
    }

    public int hashCode()
    {
        return name.hashCode();
    }

    public String toString()
 {
        String rv;
        if (isUnboxed) {
            rv = this.getCudaMemType().syntaxNameSpace() + "|" + name + "|";
        } else {
            rv = this.getCudaMemType().syntaxNameSpace() + name;
        }
        if (params != null && params.size() > 0) {
            rv += "<";
            rv += params.get(0);
            for (int i = 1; i < params.size(); ++i) {
                rv += ", " + params.get(i);
            }
            rv += ">";
        }
        return rv;
    }
    

    @Override
    public Type withMemType(CudaMemoryType memtyp) {
        return new TypeStructRef(memtyp, name, isUnboxed, params);
    }

    @Override
    public TypeComparisonResult compare(Type other) {
        if (other instanceof TypeStructRef) {
            TypeStructRef that = (TypeStructRef) other;
            return TypeComparisonResult.knownOrNeq(this.name.equals(that.name) &&
                    this.isUnboxed == that.isUnboxed);
        }

        return TypeComparisonResult.NEQ;
    }

    public Type leastCommonPromotion(Type that, NameResolver nres) {
        if (this.promotesTo(that, nres))
            return that;
        if (that.promotesTo(this, nres))
            return this;
        if ((that instanceof TypeStructRef)) {
            TypeStructRef tsr = (TypeStructRef) that;
            String name1 = nres.getStructName(tsr.name);
            if (name1 == null) {
                return null;
            }
            String name1parent = nres.getStructParentName(name1);

            String name2 = nres.getStructName(name);
            if (name2 == null) {
                return null;
            }
            String name2parent = nres.getStructParentName(name2);

            if (name1parent != null && name2parent != null) {
                return (new TypeStructRef(name2parent, isUnboxed)).leastCommonPromotion(
                        new TypeStructRef(name1parent, tsr.isUnboxed), nres);
            }
        }
        return null;
    }

    public boolean promotesTo(Type that, NameResolver nres) {
        if (super.promotesTo(that, nres))
            return true;
        if ((that instanceof TypeStructRef)) {
            TypeStructRef tsr = (TypeStructRef) that;
            String name1 = nres.getStructName(tsr.name);
            if (name1 == null) {
                return false;
            }
            String name2 = name;
            while (nres.getStructParentName(name2) != null) {
                String name3 = nres.getStructName(name2);
                if (name1.equals(name3)) {
                    return true;
                }
                name2 = nres.getStructParentName(name2);

            }
            String name3 = nres.getStructName(name2);
            return name1.equals(name3);
        } else {
            if (that instanceof TypeArray) {
                return this.promotesTo(((TypeArray) that).getBase(), nres);
            }
        }
        return false;
    }

    public Collection<Type> getBaseTypes() {
        return Collections.singletonList((Type) this);
    }


    public Map<String, Type> unify(Type t, Set<String> names) {
        if (names.contains(this.toString())) {
            // If my full name is in names, then I can just rename myself to t
            // and be equal to t.
            return Collections.singletonMap(this.toString(), t);
        } else {
            if (t instanceof TypeStructRef) {
                TypeStructRef other = (TypeStructRef) t;
                List<Type> othertp = other.getTypeParams();
                if (othertp != null && this.params != null && othertp.size() == params.size() && params.size() > 0) {
                    Iterator<Type> otherit = othertp.iterator();
                    Map<String, Type> tmap = new HashMap<String, Type>();
                    for (Type thistp : params) {
                        Type otp = otherit.next();

                        Map<String, Type> lmap = thistp.unify(otp, names);
                        for (Entry<String, Type> entr : lmap.entrySet()) {
                            // if (tmap.containsKey(entr.getKey())) {
                            // tmap.put(entr.getKey(),
                            // tmap.get(entr.getKey()).leastCommonPromotion(entr.getValue(),
                            // nres));
                            // } else {
                                tmap.put(entr.getKey(), entr.getValue());
                            // }
                        }
                    }
                    return tmap;

                }

            }
            return Collections.EMPTY_MAP;
        }
    }

    public String cleanName() {
        return this.toString().replace('@', '_');
    }

    public boolean isUnboxed() {
        return isUnboxed;
    }

}
