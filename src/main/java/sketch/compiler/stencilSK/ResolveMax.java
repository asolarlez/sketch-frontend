package sketch.compiler.stencilSK;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import sketch.compiler.ast.core.FENullVisitor;
import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.exprs.ExprArrayRange;
import sketch.compiler.ast.core.exprs.ExprBinary;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprUnary;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;


/**
 * Symbolic solver, replaces a {@link StmtMax} with a sequence of guarded assignments.
 *
 * @author asolar
 *
 */
public class ResolveMax {
	StmtMax smax;
	int sz;
	String name;
	Expression[] expArr;
	List<Expression>[] meArr;
	Integer[] tainted;

	public static Integer tag = new Integer(0);
	public ResolveMax(StmtMax smax){
		this.smax = smax;
		sz = smax.dim;
		name = smax.lhsvar;
		expArr = new Expression[sz];
		meArr = new List[sz];
		tainted = new Integer[sz];
	}

	private boolean isDone(){

		for(int i=0; i<sz; ++i){
			if( expArr[i] == null)
				return false;
		}
		return true;

	}




	/**
	 * If exprArr[i] depends on exprArr[j] for some i,j, these may lead to unwanted
	 * dependencies, so this function replaces all instances of exprArr[j] in the expression exprArr[i].
	 *
	 */
	public void removeDependencies(){
		FEReplacer repl = new FEReplacer(){
			public Object visitExprArrayRange(ExprArrayRange ea){
				ExprVar ev = ea.getAbsoluteBase();
				if( ev.getName().equals(name) ){
					int idx = ea.getOffset().getIValue();
					if( expArr[idx] != null && tainted[idx] == null){
						return expArr[idx];
					}
				}
				return ea;
			}
		};
		boolean goon = true;
		int it = 0;
		while(goon){
			goon = false;
			for(int i=0; i<sz; ++i){
				if(expArr[i] != null && tainted[i] == null ){
					Expression old = expArr[i];
					expArr[i] = (Expression) old.accept(repl);
					if(expArr[i] != old){
						goon = true;
					}
				}
			}
			++it;
			if(it > 2*sz){
				assert false : "This is probably in an infinite loop!!!";
			}
		}
	}


	public void run(){
		CollectConstraints cc = new CollectConstraints();
		{
			List<Expression> exprList = smax.primC ;
			for(Iterator<Expression> expIt = exprList.iterator(); expIt.hasNext();){
				Expression exp = expIt.next();
				exp.accept(new tagNodes());
                try {
                    exp.accept(cc);
                } catch (Exception e) {
                    // do nothing
                }
			}
		}
		if( isDone() ) return ;
		{
			List<Expression> exprList = smax.secC ;
			for(Iterator<Expression> expIt = exprList.iterator(); expIt.hasNext();){
				Expression exp = expIt.next();
				exp.accept(new tagNodes());
				exp.accept(cc);
			}
		}

		if( isDone() ) return ;

		{
			List<Expression> exprList = smax.terC ;
			for(Iterator<Expression> expIt = exprList.iterator(); expIt.hasNext();){
				Expression exp = expIt.next();
				exp.accept(new tagNodes());
                try {
                    exp.accept(cc);
                } catch (Exception e) {
                    // do nothing
                }
            }
		}

		for(int i=0; i<sz; ++i){
			if( expArr[i] == null && meArr[i].size() == 1){
				expArr[i] =  meArr[i].get(0);
			}
		}

	}


	class Taint extends FENullVisitor{

		public Object visitExprArrayRange(ExprArrayRange ea){
			ExprVar ev = ea.getAbsoluteBase();
			assert ev.getName().equals(name);
			Integer ival = ea.getOffset().getIValue();
			assert ival != null;
			tainted[ival] = tag;
			return null;
		}


	}


	/**
	 * 
	 * 
	 * @author asolar
	 *
	 */
	class CollectConstraints extends FENullVisitor{

		boolean isRoot=true;

		public Object visitExprBinary(ExprBinary exp)
	    {

	        switch(exp.getOp()){
	        case ExprBinary.BINOP_EQ: {
	        	if( exp.getTag() != null ){
		        	IsolateVars isol = new IsolateVars();
					exp.accept(isol);
					if( isRoot ){
						assert isol.idx >= 0;
						expArr[isol.idx] = isol.currentRHS;

					}else{
						assert isol.idx >= 0;
						if( meArr[isol.idx] == null) meArr[isol.idx] = new ArrayList<Expression>();
						meArr[isol.idx].add(isol.currentRHS);

					}
	        	}
	        	return null;
	        }

	        case ExprBinary.BINOP_AND:{
	        	exp.getRight().accept(this);
	        	exp.getLeft().accept(this);
	        	return null;
	        }

	        case ExprBinary.BINOP_OR:{
	        	boolean tmp = isRoot;
	        	isRoot = false;
	        	exp.getRight().accept(this);
	        	exp.getLeft().accept(this);
	        	isRoot = tmp;
	        	return null;
	        }

	        case ExprBinary.BINOP_LE:
	        case ExprBinary.BINOP_LT:
	        case ExprBinary.BINOP_GE:
	        case ExprBinary.BINOP_GT: {
	        	if( exp.getTag() != null ){
		        	IsolateVars isol = new IsolateVars();
					exp.accept(isol);
					if( isol.idx >= 0 ){
						if( meArr[isol.idx] == null) meArr[isol.idx] = new ArrayList<Expression>();
						meArr[isol.idx].add(isol.currentRHS);
					}
	        	}
	        	return null;
	        }


	        case ExprBinary.BINOP_ADD:
	        case ExprBinary.BINOP_SUB:
	        case ExprBinary.BINOP_MUL:
	        case ExprBinary.BINOP_DIV:
	        case ExprBinary.BINOP_MOD:
	        	default: throw new RuntimeException("BAD STUFF");
	        }
	    }
	}



	class IsolateVars extends FENullVisitor{

		class multivar{
			private String name;
			private int lrange;
			private int urange;
			public multivar(String name, int lrange, int urange){
				this.name = name;
				this.lrange = lrange;
				this.urange = urange;
			}
		}
		Expression currentRHS=null;
		int idx = -1;
		List<multivar> mlist = null;



		public Object handleSpecialCase(ExprBinary exp){
			throw new RuntimeException("NYI ResolveMax.handleSpecialCase");

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
	        	right = new ExprUnary( right, ExprUnary.UNOP_NEG, right);
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
	        currentRHS = new ExprBinary(null, ExprBinary.BINOP_DIV, currentRHS, right);
	        left.accept(this);
	        return null;
		}


		public Object visitExprBinaryDiv(ExprBinary exp){
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
	        assert false : "NYI";
	        currentRHS = new ExprBinary(null, ExprBinary.BINOP_DIV, currentRHS, right);
	        left.accept(this);
	        return null;
		}

		public Object visitExprUnary(ExprUnary exp){
			switch(exp.getOp()){
			case ExprUnary.UNOP_NEG: {
				currentRHS = new ExprUnary( currentRHS, ExprUnary.UNOP_NEG, currentRHS);
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
	        //case ExprBinary.BINOP_DIV: return visitExprBinaryDiv(exp);


	        case ExprBinary.BINOP_LE: return visitExprBinaryLE(exp);
	        case ExprBinary.BINOP_LT: return visitExprBinaryLT(exp);

	        case ExprBinary.BINOP_GE: return visitExprBinaryGE(exp);
	        case ExprBinary.BINOP_GT: return visitExprBinaryGT(exp);


	        case ExprBinary.BINOP_AND:
	        case ExprBinary.BINOP_DIV:
	        case ExprBinary.BINOP_MOD:
	        default: throw new RuntimeException("NYI");
	        }



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

	/**
	 * This visitor sets the tag of the expression according to the following criteria.
	 * If the expression contains an index whose value is still unknown, its tag will be set.
	 *
	 *
	 *
	 * @author asolar
	 *
	 */
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
	            return new ExprBinary(exp, exp.getOp(), left, right, exp.getAlias());
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
	            return new ExprUnary(exp, exp.getOp(), expr);
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
