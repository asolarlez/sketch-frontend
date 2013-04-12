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
import sketch.compiler.ast.core.exprs.ExprConstChar;
import sketch.compiler.ast.core.exprs.ExprConstFloat;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprNullPtr;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.cuda.typs.CudaMemoryType;

/**
 * A primitive type. This can be int, float, character, void, null, etc., depending on the
 * specified type parameter.
 * 
 * @author David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id$
 */
public class TypePrimitive extends Type
{
    /** Type constant for bit types. */
    public static final int TYPE_BIT = 1;
    /** Type constant for int types. */
    public static final int TYPE_INT8 = 2;
    public static final int TYPE_INT16 = 3;
    public static final int TYPE_INT32 = 4;
    public static final int TYPE_INT64 = 5;
    public static final int TYPE_INT = TYPE_INT32;
    /** Type constant for float types. */
    public static final int TYPE_FLOAT = 6;
    /** Type constant for double types; unused in StreamIt. */
    public static final int TYPE_DOUBLE = 7;
    /** Type constant for void types. */
    public static final int TYPE_VOID = 9;
    /** Type constant for signed integers. */
    public static final int TYPE_SIGINT = 14;

    /** Type constant for signed integers. */
    public static final int TYPE_NULLPTR = 15;

    /** For internal use only. This type can be cast to anything, and anything can be cast to it.*/
    public static final int TYPE_BOTTOM = 16;

    /** Type constant for string types */
    public static final int TYPE_CHAR = 17;


    /** Type object for bit types. */
    public static final TypePrimitive bittype = new TypePrimitive(TYPE_BIT);
    /** Type object for int types. */
    public static final TypePrimitive inttype = new TypePrimitive(TYPE_INT);
    public static final TypePrimitive int8type = new TypePrimitive(TYPE_INT8);
    public static final TypePrimitive int16type = new TypePrimitive(TYPE_INT16);
    public static final TypePrimitive int32type = new TypePrimitive(TYPE_INT32);
    public static final TypePrimitive int64type = new TypePrimitive(TYPE_INT64);
    public static final TypePrimitive siginttype = new TypePrimitive(TYPE_SIGINT);
    /** Type object for float types. */
    public static final TypePrimitive floattype =
        new TypePrimitive(TYPE_FLOAT);
    public static final TypePrimitive doubletype =
        new TypePrimitive(TYPE_DOUBLE);
    /** Type object for void types. */
    public static final TypePrimitive voidtype =
        new TypePrimitive(TYPE_VOID);
    public static final TypePrimitive nulltype =
        new TypePrimitive(TYPE_NULLPTR);

    public static final TypePrimitive bottomtype = new TypePrimitive(TYPE_BOTTOM);

    public static final TypePrimitive chartype = new TypePrimitive(TYPE_CHAR);

    private int type;

    /**
     * Create a new primitive type. It's private, because we
     * don't want other classes creating their own primitive types.
     * They should use only the ones we have defined statically above.
     *
     * This allows us to compare primitive types with simple pointer equality, without having to call the equals operator.
     * @param type  integer type number, one of the TYPE_* constants
     */
    private TypePrimitive(int type)
    {
        this(CudaMemoryType.UNDEFINED, type);
    }
    
    private TypePrimitive(CudaMemoryType cudaMemType, int type) {
        super(cudaMemType);
        this.type = type;
    }

    /**
     * Get the type number for this type.
     *
     * @return  integer type number, one of the TYPE_* constants
     */
    public int getType()
    {
        return type;
    }


    public String toString2() {
        return this.getCudaMemType().syntaxNameSpace() + toString();
    }

    public String toString()
 {
        switch (type)
        {
        case TYPE_BIT:
                return "bit";
        case TYPE_INT8:
                return "int8";
        case TYPE_INT16:
                return "int16";
        case TYPE_INT64:
                return "int64";
        case TYPE_INT:
                return "int";
        case TYPE_FLOAT:
                return "float";
        case TYPE_DOUBLE:
                return "double";
        case TYPE_VOID:
                return "void";
        case TYPE_NULLPTR:
        	return "null ptr";

            case TYPE_BOTTOM:
                return "bottom";

        case TYPE_SIGINT:
                return "int";
            case TYPE_CHAR:
                return "char";

        default:
            return "<primitive type " + type + ">";
        }
    }

    /**
     * Check if this type can be promoted to some other type.
     * Returns true if a value of this type can be assigned to
     * a variable of that type.  For primitive types, promotions
     * are ordered: boolean -> bit -> int -> float -> complex.
     *
     * @param that  other type to check promotion to
     * @return      true if this can be promoted to that
     */
    public boolean promotesTo(Type that, NameResolver nres)
    {
        if (super.promotesTo(that, nres))
            return true;
        if (!(that instanceof TypePrimitive)){
            if (that instanceof TypeArray) {
                return this.promotesTo(((TypeArray) that).getBase(), nres);
        	}else{
                if ((this.type == TYPE_NULLPTR || this.type == TYPE_BOTTOM) &&
                        that.isStruct())
        			return true;

        		return false;
        	}
        }


        int t1 = this.type;
        int t2 = ((TypePrimitive)that).type;

        if (t1 == TYPE_BOTTOM)
            return true;

        // want: "t1 < t2", more or less
        switch(t1)
 {
        case TYPE_BIT:
            return t2 == TYPE_SIGINT || t2 == TYPE_BIT || t2 == TYPE_INT ||
                        t2 == TYPE_CHAR;
        case TYPE_INT:
                return t2 == TYPE_SIGINT || t2 == TYPE_INT || t2 == TYPE_CHAR;
        case TYPE_SIGINT:
                return t2 == TYPE_SIGINT || t2 == TYPE_INT;
        case TYPE_FLOAT:
                return t2 == TYPE_FLOAT || t2 == TYPE_DOUBLE;
            case TYPE_DOUBLE:
                return t2 == TYPE_FLOAT || t2 == TYPE_DOUBLE;

        case TYPE_INT16:
        case TYPE_INT8:
        case TYPE_INT64:
                return t2 == TYPE_SIGINT || t2 == TYPE_INT;
            case TYPE_BOTTOM:
        	return true;
        case TYPE_NULLPTR:
        	return t2 == TYPE_NULLPTR;
        case TYPE_VOID:
        	return false;

            case TYPE_CHAR:
                return t2 == TYPE_INT;
        default:
            assert false : t1;
            return false;
        }
    }

    public TypeComparisonResult compare(Type that)
    {
        if (that instanceof TypePrimitive) {
            return TypeComparisonResult.knownOrNeq(this.type == ((TypePrimitive) that).type);
        }
        return TypeComparisonResult.NEQ;
    }

    public Expression defaultValue () {
        switch (type) {
    	case TYPE_BIT:
    	case TYPE_INT8:  case TYPE_INT16:  case TYPE_INT32:  case TYPE_INT64:
    	/*case TYPE_INT:*/   case TYPE_SIGINT:
    		return ExprConstInt.zero;
    	case TYPE_FLOAT: case TYPE_DOUBLE:
        	return ExprConstFloat.ZERO;
    	case TYPE_NULLPTR:
        	return ExprNullPtr.nullPtr;
    	case TYPE_VOID:
            case TYPE_BOTTOM:
    		assert false : "Type "+ type +" doesn't have a default value.";
            case TYPE_CHAR:
                return ExprConstChar.zero;
    	default:
    		assert false : "Unknown type "+ type +".";
    		return null;	// unreachable
    	}
    }

    public int hashCode()
    {
        return new Integer(type).hashCode();
    }


    public Object accept(FEVisitor visitor){
    	return visitor.visitTypePrimitive(this);
    }
    
    @Override
    public Type withMemType(CudaMemoryType memtyp) {
        return new TypePrimitive(memtyp, this.type);
    }
}
