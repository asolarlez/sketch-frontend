package streamit.frontend.stencilSK;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import streamit.frontend.nodes.ExprArray;
import streamit.frontend.nodes.ExprArrayRange;
import streamit.frontend.nodes.ExprBinary;
import streamit.frontend.nodes.ExprConstInt;
import streamit.frontend.nodes.ExprTernary;
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
	List<Expression>[] ltArr;
	List<Expression> moreConstraints = new ArrayList<Expression>();
	Integer[] tainted;
	
	public static Integer tag = new Integer(0);
	public ResolveMax(StmtMax smax){
		this.smax = smax;
		sz = smax.dim;
		name = smax.lhsvar;
		expArr = new Expression[sz];
		ltArr = new List[sz];
		tainted = new Integer[sz];
	}
	
	public void run(){
		{
			List<Expression> exprList = smax.primC ;		
			for(Iterator<Expression> expIt = exprList.iterator(); expIt.hasNext();){
				Expression exp = expIt.next();
				exp.accept(new tagNodes());
				
				if( exp.getTag() == null ){
					//moreConstraints.add(exp);
				}else{
					IsolateVars isol = new IsolateVars();
					exp.accept(isol);
					assert isol.idx >= 0;
					expArr[isol.idx] = isol.currentRHS;		
					// moreConstraints.addAll(isol.moreConstraints); // I am pretty sure I don't need this, but just in case.	
				}			
			}
		}
		
		{
			List<Expression> exprList = smax.secC ;			
			for(Iterator<Expression> expIt = exprList.iterator(); expIt.hasNext();){
				Expression exp = expIt.next();
				exp.accept(new tagNodes());				
				if( exp.getTag() == null ){
					//moreConstraints.add(exp);
				}else{
					IsolateVars isol = new IsolateVars();
					try{
						exp.accept(isol);
					}catch(Exception e){						
						exp.accept(new Taint());
					}
					if( isol.idx >= 0 && expArr[isol.idx] == null){
						if( ltArr[isol.idx] == null) ltArr[isol.idx] = new ArrayList<Expression>();
						ltArr[isol.idx].add(isol.currentRHS);
					}
				}			
			}
		}
		
		
		{
			List<Expression> exprList = smax.terC ;			
			for(Iterator<Expression> expIt = exprList.iterator(); expIt.hasNext();){
				Expression exp = expIt.next();
				exp.accept(new tagNodes());				
				if( exp.getTag() == null ){
					//moreConstraints.add(exp);
				}else{
					IsolateVars isol = new IsolateVars();
					try{
						exp.accept(isol);
					}catch(Exception e){
						exp.accept(new Taint());
					}
					if( isol.idx >= 0 && expArr[isol.idx] == null){
						if( ltArr[isol.idx] == null) ltArr[isol.idx] = new ArrayList<Expression>();
						ltArr[isol.idx].add(isol.currentRHS);
					}
				}			
			}
		}
		
		
		
	}
	
	
	class Taint extends FENullVisitor{
		public Object visitExprArray(ExprArray ea){
			assert (  ea.getBase() instanceof ExprVar ); 
			ExprVar ev = (ExprVar)ea.getBase();
			assert ev.getName().equals(name);
			Integer ival = ea.getOffset().getIValue();
			assert ival != null;
			tainted[ival] = tag;			
			return null;	
		}
		
		public Object visitExprArrayRange(ExprArrayRange ea){
			ExprVar ev = ea.getAbsoluteBase();
			assert ev.getName().equals(name);
			Integer ival = ea.getOffset().getIValue();
			assert ival != null;
			tainted[ival] = tag;			
			return null;	
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
		
		public void checkButIgnore(Expression exp){			
			int tmpidx = idx;
			exp.accept(this);
			idx = tmpidx;
			assert idx == -1;
			
		}
		

		public Object visitExprBinaryLE(ExprBinary exp){
		    Expression left = exp.getLeft();
	        Expression right = exp.getRight();
	        assert exp.getTag() != null;
	        
	        if( (left.getTag() != null && right.getTag() == null) ){
	        	currentRHS = right;	        
		        left.accept(this);
		        return null;			        	
	        }	        
	        
	        if(left.getTag() != null && right.getTag() != null)
	        	throw new RuntimeException("NYI");
	        checkButIgnore(right);
	        return null;	        			
		}
		
		public Object visitExprBinaryLT(ExprBinary exp){
		    Expression left = exp.getLeft();
	        Expression right = exp.getRight();
	        assert exp.getTag() != null;
	        
	        if( (left.getTag() != null && right.getTag() == null) ){
	        	 currentRHS = new ExprBinary(null, ExprBinary.BINOP_SUB, right, new ExprConstInt(1));
	 	        left.accept(this);
	 	        return null;	        	
	        }	   
	        if(left.getTag() != null && right.getTag() != null)
	        	throw new RuntimeException("NYI");
	        checkButIgnore(right);
	        return null;
	       			
		}
		
		public Object visitExprBinaryGE(ExprBinary exp){
		    Expression left = exp.getLeft();
	        Expression right = exp.getRight();
	        assert exp.getTag() != null;
	        
	        if( (right.getTag() != null && left.getTag() == null) ){
	        	currentRHS = left;	        
		        right.accept(this);
		        return null;			        	
	        }
	        
	        if(left.getTag() != null && right.getTag() != null)
	        	throw new RuntimeException("NYI");
	        checkButIgnore(left);
	        return null;	        			
		}
		
		public Object visitExprBinaryGT(ExprBinary exp){
		    Expression left = exp.getLeft();
	        Expression right = exp.getRight();
	        assert exp.getTag() != null;
	        
	        if( (right.getTag() != null && left.getTag() == null) ){
	        	 currentRHS = new ExprBinary(null, ExprBinary.BINOP_SUB, left, new ExprConstInt(1));
	 	        right.accept(this);
	 	        return null;	        	
	        }	   
	        if(left.getTag() != null && right.getTag() != null)
	        	throw new RuntimeException("NYI");
	        checkButIgnore(left);
	        return null;
	       			
		}
		
		
		public Object visitExprBinaryAND(ExprBinary exp){
		    Expression left = exp.getLeft();
	        Expression right = exp.getRight();
	        assert exp.getTag() != null;
	        if( left.getTag()== tag && right.getTag() == tag){
	        	//This produces multiple pieces of information, so it's not handled at this point.
	        	throw new RuntimeException("NYI");
	        }
	        
	        if( left.getTag()== null && right.getTag() == null){
	        	return null;        	
	        }
	        
	        if( left.getTag() == null){
	        	Expression tmp = left;
	        	left = right;
	        	right = tmp;	        	
	        }
	        assert left.getTag() != null && right.getTag() == null;
	        left.accept(this);
	        return null;			
		}
		
		
		public Object visitExprBinaryOR(ExprBinary exp){
		    Expression left = exp.getLeft();
	        Expression right = exp.getRight();
	        assert exp.getTag() != null;
	        if( left.getTag()== tag && right.getTag() == tag){
	        	//This is very problematic; probably won't be handled ever.
	        	throw new RuntimeException("NYI");
	        }
	        
	        if( left.getTag()== null && right.getTag() == null){
	        	return null;
	        }
	        
	        if( left.getTag() == null){
	        	Expression tmp = left;
	        	left = right;
	        	right = tmp;	        	
	        }
	        assert left.getTag() != null && right.getTag() == null;
	        left.accept(this);
	        //At this point, currentRHS has the new constraint. However, this constraint should be
	        //predicated on the rhs, because if the rhs is true, then the constraint doesn't really count.
	        //TODO need to figure out what to put in that null.
	        this.currentRHS = new ExprTernary(null, ExprTernary.TEROP_COND, new ExprUnary(null, ExprUnary.UNOP_NOT, right), left, null);
	        return null;			
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
	        
	        case ExprBinary.BINOP_LE: return visitExprBinaryLE(exp);
	        case ExprBinary.BINOP_LT: return visitExprBinaryLT(exp);
	        
	        case ExprBinary.BINOP_GE: return visitExprBinaryGE(exp);
	        case ExprBinary.BINOP_GT: return visitExprBinaryGT(exp);
	        
	        
	        case ExprBinary.BINOP_AND: return visitExprBinaryAND(exp);
	        
	        
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
		
		
		public Object visitExprArrayRange(ExprArrayRange ea){
			//assert (  ea.getBase() instanceof ExprVar ); 
			ExprVar ev = ea.getAbsoluteBase();
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
		
		public Object visitExprArrayRange(ExprArrayRange ea){
			ExprVar ev = ea.getAbsoluteBase(); 					
			if( ev.getName().equals(name) ){
				int idx = ea.getOffset().getIValue();
				if( expArr[idx] == null || (tainted[idx] != null ) ){
					ea.setTag(tag);	
				}
			}
			return ea;
		}
		
	}
}
