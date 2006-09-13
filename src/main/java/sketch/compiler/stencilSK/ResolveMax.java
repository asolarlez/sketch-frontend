package streamit.frontend.stencilSK;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import streamit.frontend.nodes.ExprArray;
import streamit.frontend.nodes.ExprBinary;
import streamit.frontend.nodes.ExprConstInt;
import streamit.frontend.nodes.ExprUnary;
import streamit.frontend.nodes.ExprVar;
import streamit.frontend.nodes.Expression;
import streamit.frontend.nodes.FENullVisitor;
import streamit.frontend.nodes.FEReplacer;



public class ResolveMax {
	StmtMax smax;
	int sz;
	String name;
	Expression[] expArr;
	List<Expression> moreConstraints = new ArrayList<Expression>();
	
	public static Integer tag = new Integer(0);
	public ResolveMax(StmtMax smax){
		this.smax = smax;
		sz = smax.dim;
		name = smax.lhsvar;
		expArr = new Expression[sz];		
	}
	
	public void run(){
		List<Expression> exprList = smax.primC ;
		
		for(Iterator<Expression> expIt = exprList.iterator(); expIt.hasNext();){
			Expression exp = expIt.next();
			exp.accept(new tagNodes());
			IsolateVars isol = new IsolateVars();
			exp.accept(isol);
			assert isol.idx >= 0;
			expArr[isol.idx] = isol.currentRHS;		
			moreConstraints.addAll(isol.moreConstraints);
		}
	}
	

	class IsolateVars extends FENullVisitor{
		
		Expression currentRHS=null;
		List<Expression> moreConstraints = new ArrayList<Expression>();
		int idx = -1;
		
		public Object handleSpecialCase(ExprBinary exp){
			throw new RuntimeException("NYI");
			
		}
		
		public void addConstraintEquals(Expression e1, Expression e2){
			moreConstraints.add(new ExprBinary(null, ExprBinary.BINOP_EQ, e1, e2));
		}
		
		
		public Object visitExprBinaryEquals(ExprBinary exp){
		    Expression left = exp.getLeft();
	        Expression right = exp.getRight();
	        assert exp.getTag() != null;
	        if( left.getTag()== tag && right.getTag() == tag){
	        	return handleSpecialCase(exp);
	        }
	        
	        if( left.getTag()== null && right.getTag() == null){
	        	throw new RuntimeException("NYI");	        	
	        }
	        
	        if( left.getTag() == null){
	        	Expression tmp = left;
	        	left = right;
	        	right = tmp;	        	
	        }
	        assert left.getTag() != null && right.getTag() == null;
	        
	        currentRHS = right;	        
	        left.accept(this);
	        return null;			
		}
		
		public Object visitExprBinaryAdd(ExprBinary exp){
		    Expression left = exp.getLeft();
	        Expression right = exp.getRight();
	        assert exp.getTag() != null;
	        if( left.getTag()== tag && right.getTag() == tag){
	        	return handleSpecialCase(exp);
	        }
	        
	        if( left.getTag()== null && right.getTag() == null){
	        	throw new RuntimeException("NYI");	        	
	        }
	        
	        if( left.getTag() == null){
	        	Expression tmp = left;
	        	left = right;
	        	right = tmp;	        	
	        }
	        assert left.getTag() != null && right.getTag() == null;
	        
	        currentRHS = new ExprBinary(null, ExprBinary.BINOP_SUB, currentRHS, right);	        
	        left.accept(this);
	        return null;			
		}
		
		public Object visitExprBinarySub(ExprBinary exp){
		    Expression left = exp.getLeft();
	        Expression right = exp.getRight();
	        assert exp.getTag() != null;
	        if( left.getTag()== tag && right.getTag() == tag){
	        	return handleSpecialCase(exp);
	        }
	        
	        if( left.getTag()== null && right.getTag() == null){
	        	throw new RuntimeException("NYI");	        	
	        }
	        
	        if( right.getTag() == null){
	        	currentRHS = new ExprBinary(null, ExprBinary.BINOP_ADD, currentRHS, right);	    
	        	left.accept(this);
	        }else{
	        	currentRHS = new ExprBinary(null, ExprBinary.BINOP_SUB, currentRHS, left);
	        	right = new ExprUnary( null, ExprUnary.UNOP_NEG, right);
	        	right.accept(this);	        	
	        }
	        return null;			
		}
		
		
		
		public Object visitExprBinaryMult(ExprBinary exp){
		    Expression left = exp.getLeft();
	        Expression right = exp.getRight();
	        assert exp.getTag() != null;
	        if( left.getTag()== tag && right.getTag() == tag){
	        	return handleSpecialCase(exp);
	        }
	        
	        if( left.getTag()== null && right.getTag() == null){
	        	throw new RuntimeException("NYI");	        	
	        }
	        
	        if( left.getTag() == null){
	        	Expression tmp = left;
	        	left = right;
	        	right = tmp;	        	
	        }
	        assert left.getTag() != null && right.getTag() == null;
	        addConstraintEquals( new ExprBinary( null,ExprBinary.BINOP_MOD, currentRHS, right), new ExprConstInt(0) );
	        currentRHS = new ExprBinary(null, ExprBinary.BINOP_DIV, currentRHS, right);   
	        left.accept(this);
	        return null;			
		}
		
		public Object visitExprUnary(ExprUnary exp){
			switch(exp.getOp()){
			case ExprUnary.UNOP_NEG: {				
				currentRHS = new ExprUnary( null, ExprUnary.UNOP_NEG, currentRHS);
				exp.getExpr().accept(this);
				return null;
			}
				
			default: 
				throw new RuntimeException("NYI");	
			}			
		}
		
		
		public Object visitExprBinary(ExprBinary exp)
	    {			
			
	        switch(exp.getOp()){
	        case ExprBinary.BINOP_EQ: return visitExprBinaryEquals(exp);
	        case ExprBinary.BINOP_ADD:  return visitExprBinaryAdd(exp);
	        case ExprBinary.BINOP_SUB:  return visitExprBinarySub(exp);
	        case ExprBinary.BINOP_MUL: return visitExprBinaryMult(exp);
	        
	        case ExprBinary.BINOP_DIV: 
	        case ExprBinary.BINOP_MOD: 	        
	        default: throw new RuntimeException("NYI");	
	        }
	        
	        
	        
	    }			

		public Object visitExprArray(ExprArray ea){
			assert (  ea.getBase() instanceof ExprVar ); 
			ExprVar ev = (ExprVar)ea.getBase();
			assert ev.getName().equals(name);
			Integer ival = ea.getOffset().getIValue();
			assert ival != null;
			this.idx = ival;
			return null;	
		}
	}
	
	
	class tagNodes extends FEReplacer{		
		public Object visitExprBinary(ExprBinary exp)
	    {
	        Expression left = doExpression(exp.getLeft());
	        Expression right = doExpression(exp.getRight());
	        if( left.getTag()== tag || right.getTag() == tag){
	        	exp.setTag(tag);
	        }
	        if (left == exp.getLeft() && right == exp.getRight())
	            return exp;
	        else
	            return new ExprBinary(exp.getContext(), exp.getOp(), left, right, exp.getAlias());
	    }
		
		 public Object visitExprUnary(ExprUnary exp)
	    {
	        Expression expr = doExpression(exp.getExpr());
	        if( exp.getExpr().getTag() == tag){
	        	exp.setTag(tag);
	        }
	        if (expr == exp.getExpr())
	            return exp;
	        else
	            return new ExprUnary(exp.getContext(), exp.getOp(), expr);
	    }
		
		public Object visitExprArray(ExprArray ea){
			if(  ea.getBase() instanceof ExprVar ){
				ExprVar ev = (ExprVar)ea.getBase();
				if( ev.getName().equals(name) ){
					ea.setTag(tag);
				}
			}
			return ea;
		}
	}
}
