package streamit.frontend.tosbit;

import streamit.frontend.nodes.ExprBinary;
import streamit.frontend.nodes.ExprConstInt;
import streamit.frontend.nodes.Expression;
import streamit.frontend.nodes.FEContext;
import streamit.frontend.nodes.FEReplacer;

public class SimplifyExpressions extends FEReplacer {

	public SimplifyExpressions() {
		super();
		// TODO Auto-generated constructor stub
	}

	public boolean isGoodOp(int op){
		switch(op){
		 case ExprBinary.BINOP_ADD: 
	        case ExprBinary.BINOP_SUB:
	        case ExprBinary.BINOP_MUL:
	        case ExprBinary.BINOP_DIV:
	        case ExprBinary.BINOP_MOD:
	        	return true;
        	default:
        		return false;
		}
	}
	
	public boolean isCompl(int op1, int op2){
		switch(op1){
		
		case ExprBinary.BINOP_ADD:{
			switch(op2){
				case ExprBinary.BINOP_SUB:
			 	case ExprBinary.BINOP_ADD:
			 		return true;
			 }
			 return false;
		}
		case ExprBinary.BINOP_SUB:{
			switch(op2){
			case ExprBinary.BINOP_SUB:
		 	case ExprBinary.BINOP_ADD:
		 		return true;
			}
			return false;
		}
	    case ExprBinary.BINOP_MUL:{
			switch(op2){
			case ExprBinary.BINOP_MUL:
		 	case ExprBinary.BINOP_DIV:
		 		return true;
			}
			return false;
		}
	    case ExprBinary.BINOP_DIV:{
			switch(op2){
			case ExprBinary.BINOP_MUL:
		 	case ExprBinary.BINOP_DIV:
		 		return true;
			}
			return false;
		}
	    case ExprBinary.BINOP_MOD:{
	    	if(op2 == ExprBinary.BINOP_MUL)
	    		return true;
	    	else
	    		return false;
	    }	        	
       	default:
       		return false;
		}
	}
	
	public Object visitExprBinary(ExprBinary exp){		
		Integer ival = exp.getIValue();
		if( ival != null ){ return new ExprConstInt(ival.intValue()); }
		if(!isGoodOp(exp.getOp())) return super.visitExprBinary(exp);		
		Expression left = doExpression(exp.getLeft());
        Expression right = doExpression(exp.getRight());
        FEContext context = exp.getCx();
        if( left instanceof ExprBinary ){
        	ExprBinary bleft = (ExprBinary) left;
        	Expression lbleft = bleft.getLeft();
        	Expression rbleft = bleft.getRight();
        	int op1 = exp.getOp();
        	int op2 = bleft.getOp();
        	if( lbleft instanceof ExprConstInt &&  op2 == ExprBinary.BINOP_MUL){
        		Expression tmp = rbleft;
        		rbleft = lbleft;
        		lbleft = tmp;
        	}
        	if( rbleft instanceof ExprConstInt ){
        		if( op2 == ExprBinary.BINOP_MUL ){
        			if(op1 == ExprBinary.BINOP_MUL){
        				if( right instanceof ExprConstInt ){
        					int nc = right.getIValue() * rbleft.getIValue();
        					return new ExprBinary(context, op1, lbleft, new ExprConstInt(nc));
        					//* Replace with a single mult with a constant
        				}else{
        					return new ExprBinary(context, op1, new ExprBinary(context, op2, lbleft, right), rbleft);
        					//* Swap. lbl && right.
        				}
        			}
        			if(op1 == ExprBinary.BINOP_DIV){
        				if( right instanceof ExprConstInt ){
        					int rblv = rbleft.getIValue();
        					int rv = right.getIValue();
        					if( rblv % rv == 0 ){
        						if( rblv == rv)
        							return lbleft;
        						else
        							return new ExprBinary(context, op2, lbleft, new ExprConstInt(rblv/rv));	
        					}
        					//* If rbl is a multiple of right,
        					//  replace with lbl*(rbl/right);
        				}
        			}
        		}
        	}
        }
        if (left == exp.getLeft() && right == exp.getRight())
            return exp;
        else
            return new ExprBinary(exp.getCx(), exp.getOp(), left, right, exp.getAlias());
	}
	
}
