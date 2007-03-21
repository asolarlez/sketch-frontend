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
 * An assert statement. Has an assertion conditional expression.
 *
 * @author  Gilad Arnold &lt;arnold@cs.berkeley.edu&gt;
 * @version
 */
public class StmtAssert extends Statement
{
    Expression cond;
    private String msg = null;
    /** Creates a new assert statement with the specified conditional. */
    public StmtAssert(FEContext context, Expression cond)
    {
        super(context);
        this.cond = cond;
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
        String result = "assert (" + this.cond + ")\n";
        return result;
    }

	/**
	 * @param msg the msg to set
	 */
	public void setMsg(String msg) {
		this.msg = msg;
	}

	/**
	 * @return the msg
	 */
	public String getMsg() {
		if(msg == null && getCx() != null) return getCx().toString(); 
		return msg;
	}
}
