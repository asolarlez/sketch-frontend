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

/**
 * An assert statement. Has an assertion conditional expression.
 * The isSuper parameter indicantes whether the assertion is "super" or not.
 * Normal assertions produce constraints of the following form:
 * assert cond => cond || ! path_conditon
 * where path_condition is the path condition to reach the constraint.
 * 
 * For super assertions, the path condition only goes up to a static funciton 
 * boundary. In other words, super assertions must hold for all calling 
 * contexts of the enclosing function, even calling contexts that may
 * not be feasible in the actual program. 
 * 
 * For example:
 * static
 * void foo(node n, i){
 *   if(i > 0){
 *      assert n != null;
 *   }
 *   ...
 * }
 * 
 * void main(){
 *   ...
 *   if(n != null){
 *      foo(n);
 *   }
 * }
 * 
 * A normal assertion would never fail when function foo is called
 * from main because the call to foo is guarded by n != null, 
 * so the constraint that you get is of the form
 * 
 * n != null || !( i>0 && n != null )
 * 
 * which is always true.
 * 
 * On the other hand, if foo were to use a super assertion instead of a normal assert,
 * then the constraint that you get is of the form
 * 
 * n != null || !(i > 0)
 * 
 * So the path constraint only goes up to the function boundary. This means
 * that the assertion must be true in all possible contexts of the function,
 * which is not the case for foo.
 * 
 * super assertions should be used with great care or not at all. The system will 
 * use them for things like putting constraints on holes that are local 
 * to a function.
 * 
 * @author  Gilad Arnold &lt;arnold@cs.berkeley.edu&gt;
 * @version
 */
public class StmtAssume extends Statement
{
    Expression cond;
    private String msg = null;


    /** @deprecated */
    public StmtAssume(FEContext context, Expression cond, String msg) {
        super(context);
        this.cond = cond;
        this.msg = msg;
    }

    public StmtAssume(FENode context, Expression cond, String msg)
    {
        super(context);
        this.cond = cond;
        this.msg = msg;
    }
    
    /** Returns the assertion condition. */
    public Expression getCond()
    {
        return cond;
    }

    /** Accepts a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        return v.visitStmtAssume(this);
    }

    /** Output to string. */
    public String toString () {
        String result = "assume (" + this.cond + ")";

        if(msg != null){
            result += ": \"" + msg + "\"";
        }

        return result;
    }

    /**
     * @param msg the msg to set
     */
    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getMsg() {
        if (msg == null || msg.isEmpty())
            return "Assume at " + getCx();
        return msg;
    }
}
