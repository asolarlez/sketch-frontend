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
 * A generic expression tree, as created in the front-end.  Expression
 * nodes often will contain other Expressions as recursive children.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&
 * @version $Id$
 */
abstract public class Expression extends FENode
{
	public Expression(Expression exp){
		super(exp);
	}

	public Expression (FENode node) {
		super (node);
	}

	/**
	 *
	 * @param context
	 * @deprecated
	 */
	public Expression(FEContext context)
    {
        super(context);
    }

    /**
     * Determine if this expression can be assigned to.  <i>The C
     * Programming Language</i> refers to expressions that can be
     * assigned to as <i>lvalues</i>, since they can appear on the
     * left-hand side of an assignment statement.
     *
     * @return true if the expression can appear on the left-hand side
     *         of an assignment statement
     */
    public boolean isLValue()
    {
        return false;
    }

    public Integer getIValue(){
    	return null;
    }

    /** Returns true iff this expression is a constant. */
    public boolean isConstant () {
    	return false;
    }

    public boolean equals(Object o){
    	if(! (o instanceof Expression) ){
    		return false;
    	}
    	Expression other = (Expression) o;
    	if(getIValue() != null && other.getIValue() != null){
    		return other.getIValue().equals(getIValue());
    	}
    	return super.equals(o);
    }
    
    public Expression doExpr(FEVisitor visit){
    	return (Expression) this.accept(visit);
    }

    // [start] subnode getters
    /** get the inner value if this is a named param */
    public Expression thisOrNamedExpr() {
        if (this instanceof ExprNamedParam) {
            return ((ExprNamedParam) this).getExpr();
        }
        return this;
    }
    // [end]
}
