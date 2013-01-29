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
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.scala.exprs.ExprConstUnit;

/**
 * A return statement with an optional value. Functions returning void (including init and
 * work functions and message handlers) should have return statements with no value;
 * functions returning a particular type should have return statements with expressions of
 * that type.
 * 
 * @author David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id$
 */
public class StmtReturn extends Statement
{
    Expression value;

    /** Creates a new return statement, with the specified return value
     * (or null). */
    public StmtReturn(FENode context, Expression value)
    {
        super(context);
        if (value instanceof ExprConstUnit) {
            value = null;
        }
        this.value = value;
    }

    /** Creates a new return statement, with the specified return value
     * (or null).
     * @deprecated
     */
    public StmtReturn(FEContext context, Expression value)
    {
        super(context);
        if (value instanceof ExprConstUnit) {
            value = null;
        }
        this.value = value;
    }

    /** Returns the return value of this, or null if there is no return
     * value. */
    public Expression getValue()
    {
        return value;
    }

    /** Accepts a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        return v.visitStmtReturn(this);
    }
    public String toString(){
        if(value != null){
            return "return " + value + ";";
        }else{
            return "return;";
        }
    }
}
