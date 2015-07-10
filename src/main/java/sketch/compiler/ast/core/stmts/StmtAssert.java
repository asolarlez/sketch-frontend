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
import java.util.Random;

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
public class StmtAssert extends Statement
{
    Expression cond;
    private String msg = null;
    public static final int NORMAL = 0;
    public static final int SUPER = 1;
    public static final int UBER = 2;
    public boolean isHard = false;
    private int superA = NORMAL;

    // isMax = 1: normal assertMax
    // isMax = 2: assertMax, but doesn't create a new group of agmax
    private int isMax;
 
    
    public int isSuper(){
        return superA;
    }
    
    public int getAssertMax() {
        return isMax;
    }
    
    public String getAssertSymbol() {
        if (isHard)
            return "hassert";
        switch (isMax) {
            case 1:
                return "assert_max";
            case 2:
                return "assert_max\\";
            default:
                return "assert";
        }
    }

    /** Creates a new assert statement with the specified conditional. */
    public StmtAssert(FENode context, Expression cond, boolean isSuper)
    {
        this (context, cond, null, isSuper);
    }

    public StmtAssert(FENode context, Expression cond, boolean isSuper, boolean isHard) {
        this(context, cond, null, isSuper);
        this.isHard = isHard;
    }

    /** Creates a new assert statement with the specified conditional. */
    public StmtAssert(Expression cond, boolean isSuper)
    {
        this (cond, cond, null, isSuper);
    }

    public StmtAssert(Expression cond, boolean isSuper, boolean isHard) {
        this(cond, cond, null, isSuper);
        this.isHard = isHard;
    }

    /** Creates a new assert statement with the specified conditional.
     * @deprecated
     */
    public StmtAssert(FEContext context, Expression cond, boolean isSuper)
    {
        this (context, cond, null, isSuper);
    }

    

    /** Creates a new assert statement with the specified conditional. */
    public StmtAssert(FENode context, Expression cond, int isSuper)
    {
        this (context, cond, null, isSuper);
    }
    
    public StmtAssert(FENode context, Expression cond, int isSuper, boolean isHard) {
        this(context, cond, null, isSuper);
        this.isHard = isHard;
    }

    public StmtAssert(FENode context, Expression cond, String msg, int isSuper)
    {
        super(context);
        this.cond = cond;
        this.msg = msg;
        this.superA = isSuper;
    }
    
    public StmtAssert(FENode context, Expression cond, String msg, int isSuper,
            boolean isHard)
    {
        super(context);
        this.cond = cond;
        this.msg = msg;
        this.superA = isSuper;
        this.isHard = isHard;
    }

    public StmtAssert(FENode context, Expression cond, String msg, boolean isSuper) {
        this(context.getCx(), cond, msg, (isSuper ? 1 : 0), 0);
    }

    public StmtAssert(FENode context, Expression cond, String msg, boolean isSuper,
            boolean isHard)
    {
        this(context.getCx(), cond, msg, (isSuper ? 1 : 0), 0);
        this.isHard = isHard;
    }

    public static StmtAssert createAssertMax(FEContext context, Expression cond,
            String msg, boolean defer)
    {
        return new StmtAssert(context, cond, msg, 0, (defer ? 2 : 1));
    }

    public static StmtAssert createAssertMax(FEContext context, Expression cond,
            String msg, boolean defer, boolean isHard)
    {
        return new StmtAssert(context, cond, msg, 0, (defer ? 2 : 1), isHard);
    }

    /**
     *
     */
    public StmtAssert(FEContext context, Expression cond, String msg, int isSuper,
            int isMax)
    {
        super(context);
        this.cond = cond;
        this.msg = msg;
        this.superA = isSuper;
        this.isMax = isMax;
    }
    
    public StmtAssert(FEContext context, Expression cond, String msg, int isSuper,
            int isMax, boolean isHard)
    {
        super(context);
        this.cond = cond;
        this.msg = msg;
        this.superA = isSuper;
        this.isMax = isMax;
        this.isHard = isHard;
    }

    public StmtAssert(FENode context, Expression cond, String msg, int isSuper,
            int isMax)
    {
        this(context.getCx(), cond, msg, isSuper, isMax);
    }

    public StmtAssert(FENode context, Expression cond, String msg, int isSuper,
            int isMax, boolean isHard)
    {
        this(context.getCx(), cond, msg, isSuper, isMax);
        this.isHard = isHard;
    }

    /**
     *
     */
    public StmtAssert(Expression cond, String msg, boolean isSuper)
    {
        this (cond, cond, msg, isSuper);
    }

    public StmtAssert(Expression cond, String msg, boolean isSuper, boolean isHard) {
        this(cond, cond, msg, isSuper);
        this.isHard = isHard;
    }

    /**
     * @deprecated
     */
    public StmtAssert(FEContext context, Expression cond, String msg, boolean isSuper)
    {
        super(context);
        this.cond = cond;
        this.msg = msg;
        this.superA = isSuper? 1 : 0;
    }

    public StmtAssert(FEContext context, Expression cond, String msg, boolean isSuper,
            boolean isHard)
    {
        super(context);
        this.cond = cond;
        this.msg = msg;
        this.superA = isSuper ? 1 : 0;
        this.isHard = isHard;
    }

    /** Returns the assertion condition. */
    public Expression getCond()
    {
        return cond;
    }

    /** Accepts a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        return v.visitStmtAssert(this);
    }

    /** Output to string. */
    public String toString () {
        String result = getAssertSymbol() + " (" + this.cond + ")";

        /*
         * XXX/cgjones: this is being cut out because asserts need to be
         * printed in the Promela code generator, but Promela doesn't support
         * the 'assert cond : message' syntax.  This can be added back in if
         * we revise the way the code generators work (all printing done within
         * the visitors).
         *
        if(msg != null){
            result += ": \"" + msg + "\"";
        }
        */


        return result;
    }

    /**
     * @param msg the msg to set
     */
    public void setMsg(String msg) {
        this.msg = msg;
    }

    private Random _my_rand = new Random();
    /**
     * @return the msg
     */
    public String getMsg() {
        if (msg == null || msg.isEmpty())
            return "Assert at " + getCx() + " (" + _my_rand.nextLong() + ")";
        return msg;
    }
}
