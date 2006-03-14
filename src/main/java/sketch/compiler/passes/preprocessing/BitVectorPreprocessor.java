package streamit.frontend.passes;

import java.util.*;

import streamit.frontend.nodes.*;
import streamit.frontend.nodes.ExprArrayRange.RangeLen;
import streamit.frontend.tosbit.EliminateStar.HasStars;

public class BitVectorPreprocessor extends SymbolTableVisitor 
{

	private static class ParameterCopyResolver extends SymbolTableVisitor 
	{
		private HashMap<String,Parameter> unmodifiedParams;

		public ParameterCopyResolver()
		{
			super(null);
		}
		
		private Function addVarCopy(Function func, Parameter param, String newName)
		{
			StmtBlock body=(StmtBlock) func.getBody();
			StmtVarDecl decl=new StmtVarDecl(func.getContext(),param.getType(),param.getName(),
					new ExprVar(func.getContext(),newName));
			List stmts=new ArrayList(body.getStmts().size()+2);
			stmts.add(decl);
			stmts.addAll(body.getStmts());
			return new Function(func.getContext(),func.getCls(),func.getName(),func.getReturnType(),
				func.getParams(),func.getSpecification(),
				new StmtBlock(body.getContext(),stmts));
		}
		@Override
		public Object visitFunction(Function func)
		{
			unmodifiedParams=new HashMap<String,Parameter>();
			for(Iterator<Parameter> iter=func.getParams().iterator();iter.hasNext();) {
				Parameter param=iter.next();
				if(param.isParameterOutput()) continue;
				unmodifiedParams.put(param.getName(),param);
			}
			Function ret=(Function) super.visitFunction(func);
			List<Parameter> parameters=func.getParams(); //assume it's mutable; for the current Function implementation that is true
			for(int i=0;i<parameters.size();i++) {
				Parameter param=parameters.get(i);
				if(param.isParameterOutput()) continue;
				if(!unmodifiedParams.containsValue(param)) {
					String newName=param.getName()+"_cp0";
					Parameter newPar=new Parameter(param.getType(),newName,param.isParameterOutput());
					parameters.set(i,newPar);
					ret=addVarCopy(ret,param,newName);
				}
			}
			return ret;
		}
		public Object visitStmtAssign(StmtAssign stmt)
		{
			Expression lhs=(Expression) stmt.getLHS().accept(this);
			while (lhs instanceof ExprArrayRange) lhs=((ExprArrayRange)lhs).getBase();
			assert lhs instanceof ExprVar;
			String lhsName=((ExprVar)lhs).getName();
			unmodifiedParams.remove(lhsName);
			return super.visitStmtAssign(stmt);
		}
		public Object visitStmtVarDecl(StmtVarDecl stmt)
		{
			int n=stmt.getNumVars();
			for(int i=0;i<n;i++) {
				unmodifiedParams.remove(stmt.getName(i));
			}
			return super.visitStmtVarDecl(stmt);
		}
	}
	
	private TempVarGen varGen;
	private HasStars starCheck;
	private ParameterCopyResolver paramCopyRes;
	
	public BitVectorPreprocessor(TempVarGen varGen) {
		super(null);
		this.varGen=varGen;
		paramCopyRes=new ParameterCopyResolver();
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
		func=(Function)func.accept(paramCopyRes);
		return super.visitFunction(func);
	}
	
}
