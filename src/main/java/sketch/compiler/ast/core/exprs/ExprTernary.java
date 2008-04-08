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

package streamit.frontend.nodes;

/**
 * A ternary expression; that is, one with three children.  C and Java
 * have exactly one of these, which is the conditional expression
 * <code>(a ? b : c)</code>, which is an expression equivalent of "if
 * (a) b else c".
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id$
 */
public class ExprTernary extends Expression
{
    // Operators: (for consistency, really)
    public static final int TEROP_COND = 1;

    private int op;
    private Expression a, b, c;

    /** Creates a new ExprTernary with the specified operation and
     * child expressions. */
    public ExprTernary(FENode context,
                       int op, Expression a, Expression b, Expression c)
    {
        super(context);
        this.op = op;
        this.a = a;
        this.b = b;
        this.c = c;
    }

    /** Creates a new ExprTernary with the specified operation and
     * child expressions. */
    public ExprTernary(String op, Expression a, Expression b, Expression c)
    {
        super(a);

        int opCode = -1;
        if ("?:".equals (op))      opCode = TEROP_COND;
        else
        	a.assertTrue (false, "Unknown ternary operator '"+ op +"'");

        this.op = opCode;
        this.a = a;
        this.b = b;
        this.c = c;
    }

    public Integer getIValue(){
    	if(op == TEROP_COND && a.getIValue()!= null && b.getIValue()!= null && c.getIValue() != null){
    		int av = a.getIValue().intValue();
    		int bv = b.getIValue().intValue();
    		int cv = c.getIValue().intValue();
    		return new Integer((av==1)?bv:cv);
    	}
    	return null;
    }
    /** Returns the operation of this. */
    public int getOp() { return op; }

    /** Returns the first child of this. */
    public Expression getA() { return a; }

    /** Returns the second child of this. */
    public Expression getB() { return b; }

    /** Returns the third child of this. */
    public Expression getC() { return c; }

    /** Accept a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        return v.visitExprTernary(this);
    }
    public String toString(){
    	return "("+ a.toString() + " ? " + b + " : " + c + ")";
    }
}
