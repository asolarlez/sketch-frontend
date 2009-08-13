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

package sketch.compiler.codegenerators.tojava;

import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEVisitor;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.typs.Type;

/**
 * A Java constructor expression.  This appears in this package
 * because it is only used for frontend to Java conversion; specifically,
 * it needs to appear at the front of init functions, before anything
 * can use variables that need to have constructors.  This is just
 * a Java 'new' expression for some single type.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id$
 */
public class ExprJavaConstructor extends Expression
{
    private Type type;

    public ExprJavaConstructor(FENode context, Type type)
    {
        super(context);
        this.type = type;
    }

    public Type getType()
    {
        return type;
    }

    public Object accept(FEVisitor v)
    {
        return v.visitOther(this);
    }
}


