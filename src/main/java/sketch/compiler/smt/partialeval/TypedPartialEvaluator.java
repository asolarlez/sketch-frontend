package sketch.compiler.smt.partialeval;

import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.ExprBinary;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.dataflow.PartialEvaluator;
import sketch.compiler.dataflow.abstractValue;
import sketch.compiler.dataflow.recursionCtrl.RecursionControl;

public class TypedPartialEvaluator extends PartialEvaluator {

	protected TypedVtype vtype;
	
	public TypedPartialEvaluator(TypedVtype vtype, TempVarGen varGen,
			boolean isReplacer, int maxUnroll, RecursionControl rcontrol) {
		super(vtype, varGen, isReplacer, maxUnroll, rcontrol);
		this.vtype = vtype;
	}
	
	
	public Object visitExprBinary(ExprBinary exp)
    {
		// this method is mostly copied from PartialEvaluator
		// the change is in the short-circuit evaluation of the AND and OR
		// this method returns CONST(false) instead of CONST(0) because of
		// the introduction of boolean type in the framework
    	abstractValue left = (abstractValue) exp.getLeft().accept(this);
    	Expression nleft =   exprRV;

        abstractValue right = null;
        Expression nright =   null;
        int op = exp.getOp();
        if(op != ExprBinary.BINOP_BAND && op != ExprBinary.BINOP_BOR && op != ExprBinary.BINOP_AND && op != ExprBinary.BINOP_OR){
        	right = (abstractValue) exp.getRight().accept(this);
        	nright =   exprRV;
        }
        abstractValue rv = null;



        switch (exp.getOp())
        {
        	case ExprBinary.BINOP_ADD: rv = vtype.plus(left, right); break;
        	case ExprBinary.BINOP_SUB: rv = vtype.minus(left, right); break;
        	case ExprBinary.BINOP_MUL: rv = vtype.times(left, right); break;
        	case ExprBinary.BINOP_DIV: rv = vtype.over(left, right); break;
        	case ExprBinary.BINOP_MOD: rv = vtype.mod(left, right); break;
        	case ExprBinary.BINOP_EQ:  rv = vtype.eq(left, right); break;
        	case ExprBinary.BINOP_NEQ: rv = vtype.not(vtype.eq(left, right)); break;
        	case ExprBinary.BINOP_LT: rv = vtype.lt(left, right); break;
        	case ExprBinary.BINOP_LE: rv = vtype.le(left, right); break;
        	case ExprBinary.BINOP_GT: rv = vtype.gt(left, right); break;
        	case ExprBinary.BINOP_GE: rv = vtype.ge(left, right); break;

        	case ExprBinary.BINOP_AND: {
        		if(left.hasIntVal() && left.getIntVal() == 0){
    				rv = vtype.CONST(false);
	    		}else{
	        		right = (abstractValue) exp.getRight().accept(this);
	        		nright =   exprRV;
	        		rv = vtype.and(left, right);
	    		}
	    		break;
        	}
        	case ExprBinary.BINOP_BAND:{
        		if(left.hasIntVal() && left.getIntVal() == 0){
        				rv = vtype.CONSTBIT(0);
        		}else{
	        		right = (abstractValue) exp.getRight().accept(this);
	        		nright =   exprRV;
	        		rv = vtype.and(left, right);
        		}
        		break;
        	}

        	case ExprBinary.BINOP_OR: {
        		if(left.hasIntVal() && left.getIntVal() == 1){
    				rv = vtype.CONST(true);
	    		}else{
	        		right = (abstractValue) exp.getRight().accept(this);
	        		nright =   exprRV;
	        		rv = vtype.or(left, right);
	    		}
	    		break;
	    	}
        	case ExprBinary.BINOP_BOR:{
        		if(left.hasIntVal() && left.getIntVal() == 1){
    				rv = vtype.CONSTBIT(1);
	    		}else{
	        		right = (abstractValue) exp.getRight().accept(this);
	        		nright =   exprRV;
	        		rv = vtype.or(left, right);
	    		}
	    		break;
	    	}

        	case ExprBinary.BINOP_BXOR: rv = vtype.xor(left, right); break;
        	case ExprBinary.BINOP_SELECT:
        		abstractValue choice = vtype.STAR(exp);
        		rv = vtype.condjoin(choice, left, right);
        		break;
        	case ExprBinary.BINOP_LSHIFT:
        		rv = vtype.shl(left, right);
        		break;
        	case ExprBinary.BINOP_RSHIFT:
        		rv = vtype.shr(left, right);
        }


        if(isReplacer){
        	if(rv.hasIntVal() ){
        		exprRV = new ExprConstInt(rv.getIntVal());
        	}else{
        		exprRV = new ExprBinary(exp, exp.getOp(), nleft, nright);
        	}
        }



        return rv;

    }
}
