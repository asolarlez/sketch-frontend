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
 * A binary expression.  This has two child expressions, which must be
 * non-null, and an operator.  The child expressions are ordered
 * (because a-b is different from b-a).
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id$
 */
public class ExprBinary extends Expression
{
    // Operators:
    public static final int BINOP_ADD = 1;
    public static final int BINOP_SUB = 2;
    public static final int BINOP_MUL = 3;
    public static final int BINOP_DIV = 4;
    public static final int BINOP_MOD = 5;
    public static final int BINOP_AND = 6;
    public static final int BINOP_OR = 7;
    public static final int BINOP_EQ = 8;
    public static final int BINOP_NEQ = 9;
    public static final int BINOP_LT = 10;
    public static final int BINOP_LE = 11;
    public static final int BINOP_GT = 12;
    public static final int BINOP_GE = 13;
    // These are bitwise AND/OR/XOR:
    public static final int BINOP_BAND = 14;
    public static final int BINOP_BOR = 15;
    public static final int BINOP_BXOR = 16;
    
    public static final int BINOP_LSHIFT = 17;
    public static final int BINOP_RSHIFT = 18;
    public static final int BINOP_SELECT = 19;
    
    private int op;
    private Expression left, right;
    private ExprBinary alias;
    /**
     * Create a new binary expression given the operation and the
     * left and right child nodes.  Requires that op is a valid
     * operator code and that left and right are non-null. 
     *
     * @param context  file and line number this expression corresponds to
     * @param op       BINOP_ operator combining the two expressions
     * @param left     expression on the left of the operator
     * @param right    expression on the right of the operator
     */
    public ExprBinary(FEContext context,
                      int op, Expression left, Expression right)
    {
        super(context);
        this.op = op;
        this.left = left;
        this.right = right;
        alias = this;
    }
    
    
    public ExprBinary(Expression left, String sop, Expression right)
{
	super(left.getCx());
	this.left = left;
	this.right = right;
	int lop = -1;
	
	if(sop.equals("+")){
		lop = BINOP_ADD;
	}else if(sop.equals("-")){
		lop = BINOP_SUB;
	}else if(sop.equals("*")){
		lop = BINOP_MUL;
	}else if(sop.equals("/")){
		lop = BINOP_DIV;
	}else if(sop.equals("%")){
		lop = BINOP_MOD;
	}else if(sop.equals("&&")){
		lop = BINOP_AND;
	}else if(sop.equals("||")){
		lop = BINOP_OR;
	}else if(sop.equals("==")){
		lop = BINOP_EQ;
	}else if(sop.equals("!=")){
		lop = BINOP_NEQ;
	}else if(sop.equals("<")){
		lop = BINOP_LT;
	}else if(sop.equals("<=")){
		lop = BINOP_LE;
	}else if(sop.equals(">")){
		lop = BINOP_GT;
	}else if(sop.equals(">=")){
		lop = BINOP_GE;
	}else if(sop.equals("&")){
		lop = BINOP_BAND;
	}else if(sop.equals("|")){
		lop = BINOP_BOR;
	}else if(sop.equals("^")){
		lop = BINOP_BXOR;
	}else if(sop.equals("xor")){
		lop = BINOP_BXOR;
	}else {
		throw new RuntimeException("What is this operator??!!");
	}
	
	this.op = lop;
	alias = this;
}
    
    

    public ExprBinary(FEContext context,
    		int op, Expression left, Expression right, ExprBinary alias)
    {
    	super(context);
    	this.op = op;
    	this.left = left;
    	this.right = right;
    	this.alias = alias;
    }
    
    public ExprBinary getAlias(){ return alias; }
    
    /**
     * Returns the operator of this.
     *
     * @return BINOP_ operator code for this expression
     */
    public int getOp() { return op; }   

    /**
     * Returns the left child expression of this.
     *
     * @return expression on the left-hand side of the operator
     */
    public Expression getLeft() { return left; }

    /**
     * Returns the right child expression of this.
     *
     * @return expression on the right-hand side of the operator
     */
    public Expression getRight() { return right; }

    /** Accept a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        return v.visitExprBinary(this);
    }

    public boolean equals(Object other)
    {
        if (!(other instanceof ExprBinary))
            return false;
        ExprBinary eb = (ExprBinary)other;
        if (!(left.equals(eb.getLeft())))
            return false;
        if (!(right.equals(eb.getRight())))
            return false;
        if (op != eb.getOp())
            return false;
        return true;
    }
    
    public int hashCode()
    {
        return left.hashCode() ^ right.hashCode() ^ new Integer(op).hashCode();
    }

    public Integer getIValue(){  
    	Integer ivI = getLeft().getIValue();
    	Integer rvI = getRight().getIValue();
    	if( ivI!= null && rvI!= null){
    		int lv = ivI.intValue();
    		int rv = rvI.intValue();
    		switch (op)
            {
            case ExprBinary.BINOP_ADD: return new Integer(lv + rv);
            case ExprBinary.BINOP_SUB: return new Integer(lv - rv);            
            case ExprBinary.BINOP_DIV: return new Integer(lv / rv);            
            case ExprBinary.BINOP_AND: return new Integer((lv==1 && rv==1)?1:0);
            case ExprBinary.BINOP_OR: return new Integer((lv==1 || rv==1)?1:0);
            case ExprBinary.BINOP_EQ: return new Integer((lv== rv)?1:0);
            case ExprBinary.BINOP_NEQ: return new Integer((lv!= rv)?1:0);
            case ExprBinary.BINOP_LT: return new Integer((lv< rv)?1:0);
            case ExprBinary.BINOP_LE: return new Integer((lv<= rv)?1:0);
            case ExprBinary.BINOP_GT: return new Integer((lv> rv)?1:0);
            case ExprBinary.BINOP_GE: return new Integer((lv >= rv)?1:0);
            case ExprBinary.BINOP_BAND: return new Integer((lv & rv));
            case ExprBinary.BINOP_BOR: return new Integer((lv | rv));
            case ExprBinary.BINOP_BXOR: return new Integer((lv ^ rv));
            case ExprBinary.BINOP_LSHIFT: return new Integer((lv << rv));
            case ExprBinary.BINOP_RSHIFT: return new Integer((lv >> rv));
            case ExprBinary.BINOP_MUL: return new Integer(lv * rv);
            case ExprBinary.BINOP_MOD: return new Integer(lv % rv);
            
            }    	
    	}
    	if( op == ExprBinary.BINOP_MOD && rvI != null){
    		if( getLeft() instanceof ExprBinary ){
    			ExprBinary left = (ExprBinary)getLeft();
    			Integer lleft = left.getLeft().getIValue();
    			Integer rleft = left.getRight().getIValue();
    			int rvIint = rvI.intValue();
    			if( left.getOp() == ExprBinary.BINOP_MUL ){
    				if(  (rleft != null && rleft.intValue()%rvIint == 0) || (lleft != null && lleft.intValue()%rvIint == 0) ){
    					return new Integer(0);
    				}
    			}
    		}
    	}
    	return null;
    }
    
    public String getOpString(){
    	String theOp;
    	switch (op)
        {
        case ExprBinary.BINOP_ADD: theOp = "+"; break;
        case ExprBinary.BINOP_SUB: theOp = "-"; break;
        case ExprBinary.BINOP_MUL: theOp = "*"; break;
        case ExprBinary.BINOP_DIV: theOp = "/"; break;
        case ExprBinary.BINOP_MOD: theOp = "%"; break;
        case ExprBinary.BINOP_AND: theOp = "&&"; break;
        case ExprBinary.BINOP_OR: theOp = "||"; break;
        case ExprBinary.BINOP_EQ: theOp = "=="; break;
        case ExprBinary.BINOP_NEQ: theOp = "!="; break;
        case ExprBinary.BINOP_LT: theOp = "<"; break;
        case ExprBinary.BINOP_LE: theOp = "<="; break;
        case ExprBinary.BINOP_GT: theOp = ">"; break;
        case ExprBinary.BINOP_GE: theOp = ">="; break;
        case ExprBinary.BINOP_BAND: theOp = "&"; break;
        case ExprBinary.BINOP_BOR: theOp = "|"; break;
        case ExprBinary.BINOP_BXOR: theOp = "^"; break;
        case ExprBinary.BINOP_LSHIFT: theOp = "<<"; break;
        case ExprBinary.BINOP_RSHIFT: theOp = ">>"; break;
        case ExprBinary.BINOP_SELECT: theOp = "{|}"; break;
        default: theOp = "? (" + op + ")"; break;
        }
    	return theOp;
    }
    
    public String toString()
    {
        String theOp = getOpString();        
        return "(" + left.toString() + ")" + theOp +
            "(" + right.toString() + ")";
    }
}
