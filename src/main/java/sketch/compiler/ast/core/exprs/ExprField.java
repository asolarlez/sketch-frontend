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

package sketch.compiler.ast.core.exprs;
import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEVisitor;
import sketch.compiler.ast.core.typs.Type;

/**
 * A reference to a named field of a <code>TypeStruct</code> node. This is the expression
 * "foo.bar". It contains a "left" expression ("foo") and the name of the field being
 * referenced ("bar").
 * 
 * @author David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id$
 */
public class ExprField extends Expression
{
    private Expression left;
    private String name;
    private boolean hole;
    private Type typeOfHole = null;
    private boolean isLValue = true;

    /** Creates a new field-reference expression, referencing the
     * named field of the specified expression. */
    public ExprField(FENode context, Expression left, String name, boolean hole)
    {
        super(context);
        this.left = left;
        this.name = name;
        this.hole = hole;
    }

    public ExprField(FENode context, Expression left, String name) {
        super(context);
        this.left = left;
        this.name = name;
        this.hole = false;
    }

    /** Creates a new field-reference expression, referencing the
     * named field of the specified expression. */
    public ExprField(Expression left, String name, boolean hole)
    {
        this(left, left, name, hole);
    }

    public ExprField(Expression left, String name) {
        this(left, left, name, false);
    }

    public boolean isHole() {
        return hole;
    }

    public Type getTypeOfHole() {
        return typeOfHole;
    }

    public void setTypeOfHole(Type t) {
        typeOfHole = t;
    }
    /** Returns the expression we're taking a field from. */
    public Expression getLeft() { return left; }

    /** Returns the name of the field. */
    public String getName() { return name; }

    /** Accept a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        return v.visitExprField(this);
    }

    /**
     * Determine if this expression can be assigned to. Fields can always be assigned to.
     * Not if the struct is immutable
     * 
     * @return always true
     */
    public boolean isLValue()
    {
        return isLValue;
    }

    public void setIsLValue(boolean val) {
        isLValue = val;
    }

    public String toString()
    {
        return left + "." + name;
    }

    public boolean equals(Object other)
    {
        if (!(other instanceof ExprField))
            return false;
        ExprField that = (ExprField)other;
        if (!(this.left.equals(that.left)))
            return false;
        if (!(this.name.equals(that.name)))
            return false;
        return true;
    }

    public int hashCode()
    {
        return left.hashCode() ^ name.hashCode();
    }
}
