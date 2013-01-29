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
import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEVisitor;

/**
 * A single-character literal, as appears inside single quotes in Java.
 * 
 * @author David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id$
 */
public class ExprConstChar extends ExprConstant
{
    private char val;
    public static final ExprConstChar zero = new ExprConstChar((FENode) null, '\0');

    /** Create a new ExprConstChar for a particular character. */

    public ExprConstChar(FENode context, char val)
    {
        super(context);
        this.val = val;
    }

    /**
     * Create a new ExprConstChar for a particular character.
     * @deprecated
     */
    public ExprConstChar(FEContext context, char val)
    {
        super(context);
        this.val = val;

    }

    public static char readChar(String str) {
        if (str.charAt(1) == '\\') {
            switch (str.charAt(2)) {
                case 'n':
                    return '\n';
                case 'r':
                    return '\r';
                case '0':
                    return 0;
                case '\\':
                    return '\\';
                case '\'':
                    return '\'';
            }
            return 0;
        } else {
            return str.charAt(1);
        }
    }

    /**
     * Create a new ExprConstChar containing the first character of a String.
     */
    public ExprConstChar(FENode context, String str) {
        super(context);
        this.val = readChar(str);
    }

    /**
     * Create a new ExprConstChar containing the first character of a String.
     * 
     * @deprecated
     */
    public ExprConstChar(FEContext context, String str)
    {
        super(context);
        this.val = readChar(str);
    }

    /** Returns the value of this. */
    public char getVal() {
        return val;
    }

    public String toString() {
        if (val == 0) {
            return "\'\\0\'";
        }
        if (val == '\n') {
            return "\'\\n\'";
        }
        if (val == '\r') {
            return "\'\\r\'";
        }
        if (val == '\'') {
            return "\'\\\'\'";
        }
        if (val == '\\') {
            return "\'\\\\\'";
        }
        String tmp = "\'" + val + "\'";

        return tmp;
    }

    /** Accept a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        return v.visitExprConstChar(this);
    }
}
