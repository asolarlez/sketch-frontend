package streamit.frontend.passes;

import java.util.*;

import streamit.frontend.nodes.*;
import streamit.frontend.nodes.ExprArrayRange.RangeLen;
import streamit.frontend.tosbit.EliminateStar.HasStars;

public class BitVectorPreprocessor extends SymbolTableVisitor 
{
	private TempVarGen varGen;
	private HasStars starCheck;
	
	public BitVectorPreprocessor(TempVarGen varGen) {
		super(null);
		this.varGen=varGen;
	}
	
	private static boolean isBitType(Type t)
    {
    	return t instanceof TypePrimitive && ((TypePrimitive)t).getType()==TypePrimitive.TYPE_BIT;
    }

    private static boolean isBitArrayType(Type t)
    {
    	return t instanceof TypeArray && isBitType(((TypeArray)t).getBase());
    }

    private boolean isLongVector(Type t) {
		if(t instanceof TypeArray) {
			TypeArray array=(TypeArray) t;
			if(isBitType(array.getBase()) && array.getLength() instanceof ExprConstInt) {
				ExprConstInt len=(ExprConstInt) array.getLength();
				if(len.getVal()<=64) return false;
			}
			return true;
		}
		return false;
	}

	private boolean isLongVector(Expression e) {
		if(e instanceof ExprVar)
			return isLongVector(symtab.lookupVar((ExprVar)e));
		return false;
	}

	private Expression makeTempExpr(Expression e)
	{
		final FEContext ct=e.getContext();
		final String tmp=varGen.nextVar();
		StmtAssign assign=new StmtAssign(ct,new ExprVar(ct,tmp),e);
		assign=(StmtAssign) visitStmtAssign(assign);
		addStatement(assign);
		return new ExprVar(ct,tmp);
	}

	@Override
	public Object visitStmtAssign(StmtAssign stmt)
	{
		Expression lhs=(Expression) stmt.getLHS().accept(this);
		if(isLongVector(lhs)) {
			//convert complicated vector assignments to a sequence of simple assignments
			if(stmt.getRHS() instanceof ExprBinary)
			{
				ExprBinary rhs=(ExprBinary) stmt.getRHS();
				Expression left=rhs.getLeft();
				Expression right=rhs.getRight();
				if(left instanceof ExprBinary) {
					left=makeTempExpr(left);
				}
				if(right instanceof ExprBinary) {
					right=makeTempExpr(right);
				}
				left=(Expression) left.accept(this);
				right=(Expression) right.accept(this);
				
				if(left!=rhs.getLeft() || right!=rhs.getRight()) {
					Expression newExpr=new ExprBinary(rhs.getContext(),rhs.getOp(),left,right);
					newExpr=(Expression) newExpr.accept(this);
					return new StmtAssign(stmt.getContext(),stmt.getLHS(),newExpr);
				}
			}
		}
		return super.visitStmtAssign(stmt);
	}

	@Override
	public Object visitExprArray(ExprArray exp) {
		return new ExprArrayRange(exp.getBase(),Collections.singletonList(
			new RangeLen(exp.getOffset()))).accept(this);
    }

	@Override
	public Object visitExprBinary(ExprBinary exp)
	{
		if(exp.getOp()==ExprBinary.BINOP_LSHIFT)
			exp=new ExprBinary(exp.getContext(),ExprBinary.BINOP_RSHIFT,exp.getLeft(),exp.getRight());
		else if(exp.getOp()==ExprBinary.BINOP_RSHIFT)
			exp=new ExprBinary(exp.getContext(),ExprBinary.BINOP_LSHIFT,exp.getLeft(),exp.getRight());
		return super.visitExprBinary(exp);
	}

	@Override
	public Object visitStmtVarDecl(StmtVarDecl stmt)
	{
		//convert all array initializations to separate assignments
		int n=stmt.getNumVars();
		int na=0;
		for(int i=0;i<n;i++) if(isBitArrayType(stmt.getType(i))) na++;
		if(na==0) return super.visitStmtVarDecl(stmt);
		
		List types=new ArrayList(3);
		List names=new ArrayList(3);
		List inits=new ArrayList(3);
		List<Statement> statements=new ArrayList<Statement>();
		for(int i=0;i<n;i++) {
			if(isBitArrayType(stmt.getType(i))) {
				String name=stmt.getName(i);
				StmtVarDecl decl=new StmtVarDecl(stmt.getContext(),stmt.getType(i),name,null);
				decl=(StmtVarDecl) super.visitStmtVarDecl(decl);
				statements.add(decl);
				Expression initExpr=stmt.getInit(i);
				if(initExpr==null && !name.startsWith("__"))
					initExpr=new ExprConstInt(stmt.getContext(),0);
				if(initExpr!=null) { 
					StmtAssign let=new StmtAssign(stmt.getContext(),new ExprVar(stmt.getContext(),name),initExpr);
					statements.add((Statement)let.accept(this));
				}
			}
			else {
				types.add(stmt.getType(i));
				names.add(stmt.getName(i));
				inits.add(stmt.getInit(i));
			}
		}
		if(n-na>0) {
			StmtVarDecl decl=new StmtVarDecl(stmt.getContext(),types,names,inits);
			decl=(StmtVarDecl) super.visitStmtVarDecl(decl);
			addStatement(decl);
		}
		addStatements(statements);
		return null;
	}
	
	@Override
	public Object visitStreamSpec(StreamSpec spec)
	{
		starCheck=new HasStars(spec);
		return super.visitStreamSpec(spec);
	}

	@Override
	public Object visitFunction(Function func)
	{
		if(func.getSpecification()==null && starCheck.testNode(func))
			return null;
		return super.visitFunction(func);
	}
	
}
