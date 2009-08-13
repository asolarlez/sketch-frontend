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

package sketch.compiler.ast.core.stmts;
import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEVisitor;
import sketch.compiler.ast.core.exprs.ExprBinary;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.typs.TypePrimitive;

/**
 * A C-style for loop.  The loop contains an initialization statement,
 * a loop condition, and an increment statement.  On entry, the
 * initialization statement is executed, and then the condition is
 * evaluated.  If the condition is true, the body is executed,
 * followed by the increment statement.  This loops until the
 * condition returns false.  Continue statements cause control flow to
 * go to the increment statement.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id$
 */
public class StmtFor extends Statement
{
    private Expression cond;
    private Statement init, incr, body;

    /** Creates a new for loop. */
    public StmtFor(FENode context, Statement init, Expression cond,
                   Statement incr, Statement body)
    {
        super(context);
        this.init = init;
        this.cond = cond;
        this.incr = incr;
        this.body = body;
    }

    /** Creates a new for loop.
     * @deprecated
     */
    public StmtFor(FEContext context, Statement init, Expression cond,
                   Statement incr, Statement body)
    {
        super(context);
        this.init = init;
        this.cond = cond;
        this.incr = incr;
        this.body = body;
    }

    /** Creates a for loop of the form for(int iterName=0; iterName &lt; upperBound; ++iterName){body} */
    public StmtFor(String iterName, Expression upperBound, Statement body)
    {
        super(upperBound);
        this.init = new StmtVarDecl(upperBound, TypePrimitive.inttype, iterName, ExprConstInt.zero);
        Expression iterVar = new ExprVar(upperBound, iterName);
        this.cond = new ExprBinary(upperBound, ExprBinary.BINOP_LT, iterVar, upperBound);
        this.incr = new StmtAssign(iterVar,  new ExprBinary(upperBound, ExprBinary.BINOP_ADD, iterVar, ExprConstInt.one));
        this.body = body;
    }



    /** Return the initialization statement of this. */
    public Statement getInit()
    {
        return init;
    }

    /** Return the loop condition of this. */
    public Expression getCond()
    {
        return cond;
    }

    /** Return the increment statement of this. */
    public Statement getIncr()
    {
        return incr;
    }

    /** Return the loop body of this. */
    public Statement getBody()
    {
        return body;
    }

    /** Accept a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        return v.visitStmtFor(this);
    }

    public String toString(){
    		String result = "for(...){\n";
    		result += this.body + "}\n";
    		return result;
    }
}

