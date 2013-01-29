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
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

import sketch.compiler.ast.core.Annotation;
import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.FEVisitor;
import sketch.compiler.ast.core.exprs.ExprNullPtr;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.cuda.typs.CudaMemoryType;
import sketch.util.datastructures.HashmapList;
import sketch.util.datastructures.ImmutableTypedHashMap;
import sketch.util.datastructures.ObjPairBase;
import sketch.util.datastructures.TypedHashMap;

/**
 * A hetereogeneous structure type. This type has a name for itself, and an ordered list
 * of field names and types. You can retrieve the list of names, and the type a particular
 * name maps to. The names must be unique within a given structure.
 * <p>
 * There is an important assumption in testing for equality and type promotions: two
 * structure types are considered equal if they have the same name, regardless of any
 * other characteristics. This allows structures and associated structure-reference types
 * to sanely compare equal.
 * <p>
 * This is the "definition" of a structure which contains not only the struct name, but
 * also the information of its fields. On the contrary, <code>TypeStructRef</code> just
 * refers to a struct by its name.
 * 
 * @author David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id$
 */
public class TypeStruct extends Type implements Iterable<Entry<String, Type>>
{
    private final FEContext context;
    private final String name;
    private String pkg;
    private final ImmutableTypedHashMap<String, Type> fieldTypMap;
    private HashmapList<String, Annotation> annotations =
            new HashmapList<String, Annotation>();

    public Set<Entry<String, Vector<Annotation>>> annotationSet() {
        return annotations.entrySet();
    }

    public boolean hasAnnotation(String tag) {
        return annotations.containsKey(tag);
    }

    public Vector<Annotation> getAnnotation(String tag) {
        return annotations.getOrEmpty(tag);
    }

    public static class TStructCreator {
        private String name;
        private String pkg;
        private ImmutableTypedHashMap<String, Type> fieldTypMap;
        private Object base;
        private HashmapList<String, Annotation> annotations;
        private CudaMemoryType typ = CudaMemoryType.UNDEFINED;

        public TStructCreator(FEContext ts) {
            base = ts;
        }

        public TStructCreator(TypeStruct ts) {
            base = ts;
            name = ts.name;
            pkg = ts.pkg;
            fieldTypMap = ts.fieldTypMap;
            annotations = ts.annotations;
            typ = ts.getCudaMemType();
        }

        public TStructCreator cmt(CudaMemoryType typ) {
            this.typ = typ;
            return this;
        }
        public TStructCreator name(final String name) {
            this.name = name;
            return this;
        }

        public TStructCreator pkg(final String pkg) {
            this.pkg = pkg;
            return this;
        }

        public TStructCreator fields(List<String> fields, List<Type> ftypes) {
            TypedHashMap<String, Type> types = new TypedHashMap<String, Type>();
            for (int i = 0; i < fields.size(); i++)
                types.put(fields.get(i), ftypes.get(i));
            this.fieldTypMap = types.immutable();
            return this;
        }

        public TStructCreator fields(ImmutableTypedHashMap<String, Type> fieldTypMap) {
            this.fieldTypMap = fieldTypMap;
            return this;
        }

        public TStructCreator fields(TypedHashMap<String, Type> fieldTypMap) {
            this.fieldTypMap = fieldTypMap.immutable();
            return this;
        }

        public TStructCreator annotations(HashmapList<String, Annotation> annotations) {
            this.annotations = annotations;
            return this;
        }

        public TypeStruct create() {
            if (base == null || base instanceof FEContext) {
                return new TypeStruct(typ, (FEContext) base, name, pkg, fieldTypMap,
                        annotations);
            } else {
                return new TypeStruct(typ, ((TypeStruct) base).getContext(), name, pkg,
                        fieldTypMap,
                        annotations);
            }
        }
    }

    /**
     * This method should only be called by the parser when the TypeStruct is first
     * created. After that, the field should be considered immutable.
     * 
     * @param pkg
     */
    public void setPkg(String pkg) {
        this.pkg = pkg;
    }

    public static TStructCreator creator(FEContext ctx, String name, List<String> fields,
            List<Type> ftypes, HashmapList<String, Annotation> annotations)
    {
        return (new TStructCreator(ctx)).name(name).fields(fields, ftypes).annotations(
                annotations);
    }

    public TStructCreator creator() {
        return (new TStructCreator(this));
    }

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
    public TypeStruct(CudaMemoryType typ, FEContext context, String name, String pkg,
            List<String> fields, List<Type> ftypes,
            HashmapList<String, Annotation> annotations)
    {
        super(typ);
        this.context = context;
        this.name = name;
        this.pkg = pkg;
        TypedHashMap<String, Type> types = new TypedHashMap<String, Type>();
        for (int i = 0; i < fields.size(); i++)
            types.put(fields.get(i), ftypes.get(i));
        this.fieldTypMap = types.immutable();
        this.annotations = annotations;
    }

    public TypeStruct(CudaMemoryType typ, FEContext context, String name, String pkg,
            TypedHashMap<String, Type> map, HashmapList<String, Annotation> annotations)
    {
        super(typ);
        this.context = context;
        this.name = name;
        this.pkg = pkg;
        this.fieldTypMap = map.immutable();
        this.annotations = annotations;
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

    public String getFullName() {
        return name + '@' + pkg;
    }

    /**
     * Returns the number of fields.
     *
     * @return the number of fields in the structure
     */
    public int getNumFields()
    {
        return getFieldTypMap().size();
    }

    public Collection<String> getFields ()
    {
        return fieldTypMap.keySet();
    }

    /**
     * Returns the type of the field with the specified name.
     *
     * @param f the name of the field to get the type of
     * @return  the type of the field named f
     */
    public Type getType(String f)
    {
        return getFieldTypMap().get(f);
    }

    /** Return true iff F is a field of this struct. */
    public boolean hasField (String f) {
        return getFieldTypMap().containsKey(f);
    }

    public Expression defaultValue () {
    	return ExprNullPtr.nullPtr;
    }

    /** Accept a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        return v.visitTypeStruct (this);
    }

    @Override
    public TypeComparisonResult compare(Type other) {
        if (other instanceof TypeStruct) {
            TypeStruct that = (TypeStruct) other;
            return TypeComparisonResult.knownOrNeq(name.equals(that.getName()));
        }

        if (other instanceof TypeStructRef) {
            TypeStructRef that = (TypeStructRef) other;
            return TypeComparisonResult.knownOrNeq(this.name.equals(that.getName()));
        }

        return TypeComparisonResult.NEQ;
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
        for (Entry<String, Type> ent : this.getFieldTypMap().entrySet()) {
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
        /*
         * if updating is desired TypedHashMap<String, Type> fields2 = new
         * TypedHashMap<String, Type>(); for (Entry<String, Type> v :
         * fieldTypMap.entrySet()) { fields2.put(v.getKey(),
         * v.getValue().withMemType(memtyp)); }
         */
        return new TypeStruct(memtyp, context, name, pkg, fieldTypMap, annotations);
    }

    public ImmutableTypedHashMap<String, Type> getFieldTypMap() {
        return fieldTypMap;
    }

    public Iterator<Entry<String, Type>> iterator() {
        return fieldTypMap.iterator();
    }
}

