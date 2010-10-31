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
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.Map.Entry;

import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.FEVisitor;
import sketch.compiler.ast.core.exprs.ExprNullPtr;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.cuda.typs.CudaMemoryType;
import sketch.util.datastructures.ObjPairBase;

/**
 * A hetereogeneous structure type.  This type has a name for itself,
 * and an ordered list of field names and types.  You can retrieve the
 * list of names, and the type a particular name maps to.  The names
 * must be unique within a given structure.
 *<p>
 * There is an important assumption in testing for equality and
 * type promotions: two structure types are considered equal if
 * they have the same name, regardless of any other characteristics.
 * This allows structures and associated structure-reference types
 * to sanely compare equal.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id$
 */
public class TypeStruct extends Type
{
    private FEContext context;
    private String name;
    private List<String> fields;
    private Map<String, Type> types;

    /**
     * Creates a new structured type. The fields and ftypes lists must be the same length;
     * a field in a given position in the fields list has the type in the equivalent
     * position in the ftypes list.
     * 
     * @param context
     *            file and line number the structure was declared in
     * @param name
     *            name of the structure
     * @param fields
     *            list of <code>String</code> containing the names of the fields
     * @param ftypes
     *            list of <code>Type</code> containing the types of the fields
     */
    public TypeStruct(CudaMemoryType typ, FEContext context, String name,
            List<String> fields, List<Type> ftypes)
    {
        super(typ);
        this.context = context;
        this.name = name;
        this.fields = fields;
        this.types = new HashMap<String, Type>();
        for (int i = 0; i < fields.size(); i++)
            this.types.put(fields.get(i), ftypes.get(i));
    }

    public TypeStruct(FEContext context, String name, List<String> fields,
            List<Type> ftypes)
    {
        this(CudaMemoryType.UNDEFINED, context, name, fields, ftypes);
    }

    public boolean isStruct () { return true; }

    /**
     * Returns the context of the structure in the original source code.
     *
     * @return file name and line number the structure was declared in
     */
    public FEContext getContext()
    {
        return context;
    }

    /**
     * Returns the name of the structure.
     *
     * @return the name of the structure
     */
    public String getName()
    {
        return name;
    }

    /**
     * Returns the number of fields.
     *
     * @return the number of fields in the structure
     */
    public int getNumFields()
    {
        return fields.size();
    }

    /**
     * Returns the name of the specified field.
     *
     * @param n zero-based index of the field to get the name of
     * @return  the name of the nth field
     */
    public String getField(int n)
    {
        return fields.get(n);
    }

    public Collection<String> getFields ()
    {
    	return Collections.unmodifiableCollection (fields);
    }

    /**
     * Returns the type of the field with the specified name.
     *
     * @param f the name of the field to get the type of
     * @return  the type of the field named f
     */
    public Type getType(String f)
    {
        return types.get(f);
    }

    /** Return true iff F is a field of this struct. */
    public boolean hasField (String f) {
    	return types.containsKey (f);
    }

    /**
     * Set the type of field 'f' to 't'.
     *
     * @param f
     * @param t
     * @deprecated
     */
    public void setType (String f, Type t) {
    	types.put (f, t);
    }

    public Expression defaultValue () {
    	return ExprNullPtr.nullPtr;
    }

    /** Accept a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        return v.visitTypeStruct (this);
    }

    // Remember, equality and such only test on the name.
    public boolean equals(Object other)
    {
        if (other instanceof TypeStruct)
        {
            TypeStruct that = (TypeStruct)other;
            return this.name.equals(that.name);
        }

        if (other instanceof TypeStructRef)
        {
            TypeStructRef that = (TypeStructRef)other;
            return name.equals(that.getName());
        }

        if (this.isComplex() && other instanceof Type)
            return ((Type)other).isComplex();

        return false;
    }

    public int hashCode()
    {
        return name.hashCode();
    }

    public String toString()
    {
        return name + "_ST";
    }

    public List<StructFieldEnt> getFieldEntries() {
        Vector<StructFieldEnt> result = new Vector<StructFieldEnt>();
        for (Entry<String, Type> ent : this.types.entrySet()) {
            result.add(new StructFieldEnt(ent.getKey(), ent.getValue()));
        }
        return result;
    }

    // [start] StructFieldEnt = (String, Type)
    public static class StructFieldEnt extends ObjPairBase<String, Type> {
        public StructFieldEnt(String left, Type right) {
            super(left, right);
        }
        
        public String getName() { return left; }
        public Type getType() { return right; }
    }
    // [end]

    @Override
    public Type withMemType(CudaMemoryType memtyp) {
        List<Type> typesLst = new ArrayList<Type>();
        for (String s : fields) {
            typesLst.add(types.get(s));
        }
        return new TypeStruct(memtyp, context, name, fields, typesLst);
    }
}

