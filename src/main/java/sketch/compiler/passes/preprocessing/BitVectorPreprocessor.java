package sketch.compiler.passes.preprocessing;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.Package;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.ExprArrayInit;
import sketch.compiler.ast.core.exprs.ExprBinary;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprStar;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.passes.lowering.SymbolTableVisitor;

public class BitVectorPreprocessor extends SymbolTableVisitor
{
	public static class HasStars extends FEReplacer{
		private Package ss;
		boolean hasUnknown=false;
		private Set<Function> visitedFunctions = new HashSet<Function>();;
		public HasStars(Package ss) {
			this.ss=ss;
		}
		public Object visitExprFunCall(ExprFunCall exp)
	    {
            Function fun = nres.getFun(exp.getName());
			assert fun != null : "Calling undefined function!!";
			Object obj = super.visitExprFunCall(exp);
			if(!visitedFunctions.contains(fun)){
				visitedFunctions.add(fun);
				fun.accept(this);
			}
			return obj;
	    }

		public Object visitExprBinary(ExprBinary exp)
	    {
			if(exp.getOp() == ExprBinary.BINOP_SELECT){
				hasUnknown = true;
			}
			return super.visitExprBinary(exp);
	    }
		public Object visitExprStar(ExprStar star) {
			hasUnknown = true;
			return star;
		}
		public boolean testNode(FENode node){
			this.visitedFunctions.clear();
			hasUnknown = false;
			node.accept(this);
			return hasUnknown;
		}
	}
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

	private Expression makeTempExpr(Expression e, Type type)
	{
		final FENode ct=e;
		final String tmp=varGen.nextVar();
		Expression init = null;
		if( type instanceof TypeArray ){
			List<Expression> ilist = new ArrayList<Expression>();
			int N = ((TypeArray)type).getLength().getIValue();
			for(int i=0; i<N; ++i){
				ilist.add( ExprConstInt.zero );
			}
			init = new ExprArrayInit(e, ilist);
		}else{

			init = ExprConstInt.zero;
		}

		StmtVarDecl svd = new StmtVarDecl(e, type, tmp, init );
		addStatement((Statement)super.visitStmtVarDecl(svd));
		StmtAssign assign=new StmtAssign(new ExprVar(ct,tmp),e);
		assign=(StmtAssign) visitStmtAssign(assign);
		addStatement(assign);
		return new ExprVar(ct,tmp);
	}
	private int bitLength(Type type){
		if( type instanceof TypeArray){
			return ((TypeArray) type).getLength().getIValue();
		}
		if( isBitType(type) ){
			return 1;
		}
		assert false;
		return -1;

	}
	@Override
	public Object visitStmtAssign(StmtAssign stmt)
	{
		Expression lhs=(Expression) stmt.getLHS().accept(this);
		Type lhsType = getType(lhs);
		if(isBitArrayType(lhsType) || isBitType(lhsType)) {
			//convert complicated vector assignments to a sequence of simple assignments
			if(stmt.getRHS() instanceof ExprBinary)
			{
				ExprBinary rhs=(ExprBinary) stmt.getRHS();
				Expression left=rhs.getLeft();
				Expression right=rhs.getRight();
				if(left instanceof ExprConstInt){
					switch(rhs.getOp()) {
					case ExprBinary.BINOP_ADD:
					case ExprBinary.BINOP_AND:
					case ExprBinary.BINOP_BAND:
					case ExprBinary.BINOP_BOR:
					case ExprBinary.BINOP_BXOR:
					case ExprBinary.BINOP_DIV:
					case ExprBinary.BINOP_LSHIFT:
					case ExprBinary.BINOP_MOD:
					case ExprBinary.BINOP_MUL:
					case ExprBinary.BINOP_OR:
					case ExprBinary.BINOP_RSHIFT:
					case ExprBinary.BINOP_SELECT:
					case ExprBinary.BINOP_SUB:
						int sz = bitLength(lhsType);
						List<Expression> lst=new ArrayList<Expression>();
						for(int i=0; i<sz; ++i){ lst.add(left); }
						left= new ExprArrayInit(stmt, lst);
						break;
					}
				}
				if(right instanceof ExprConstInt) {
					switch(rhs.getOp()) {
					case ExprBinary.BINOP_ADD:
					case ExprBinary.BINOP_AND:
					case ExprBinary.BINOP_BAND:
					case ExprBinary.BINOP_BOR:
					case ExprBinary.BINOP_BXOR:
					case ExprBinary.BINOP_DIV:
					case ExprBinary.BINOP_MOD:
					case ExprBinary.BINOP_MUL:
					case ExprBinary.BINOP_OR:
					case ExprBinary.BINOP_SELECT:
					case ExprBinary.BINOP_SUB:
						int sz = bitLength(lhsType);
						List<Expression> lst=new ArrayList<Expression>();
						for(int i=0; i<sz; ++i){ lst.add(right); }
						right= new ExprArrayInit(stmt, lst);
						break;
					}
				}
				if(left instanceof ExprBinary) {
					left=makeTempExpr(left, getType(left));
				}
				if(right instanceof ExprBinary) {
					right=makeTempExpr(right,  getType(right));
				}
				left=(Expression) left.accept(this);
				right=(Expression) right.accept(this);

				if(left!=rhs.getLeft() || right!=rhs.getRight()) {
					Expression newExpr=new ExprBinary(rhs,rhs.getOp(),left,right);
					newExpr=(Expression) newExpr.accept(this);
					return new StmtAssign(lhs,newExpr);
				}
			}
		}
/*		if( isBitArrayType(lhsType)){
			if(stmt.getRHS() instanceof ExprBinary)
			{
				ExprBinary rhs=(ExprBinary) stmt.getRHS();
				Expression left=rhs.getLeft();
				Expression right=rhs.getRight();
				if(left instanceof ExprConstInt){
					int sz = ((TypeArray) lhsType).getLength().getIValue();
					List<Expression> lst=new ArrayList<Expression>();
					for(int i=0; i<sz; ++i){ lst.add(left); }
					left= new ExprArrayInit(stmt, lst);
				}
				if(right instanceof ExprConstInt && !(rhs.getOp() == ExprBinary.BINOP_LSHIFT || rhs.getOp() == ExprBinary.BINOP_RSHIFT)) {
					int sz = ((TypeArray) lhsType).getLength().getIValue();
					List<Expression> lst=new ArrayList<Expression>();
					for(int i=0; i<sz; ++i){ lst.add(right); }
					right= new ExprArrayInit(stmt, lst);
				}
				left=(Expression) left.accept(this);
				right=(Expression) right.accept(this);
				if(left!=rhs.getLeft() || right!=rhs.getRight()) {
					Expression newExpr=new ExprBinary(rhs,rhs.getOp(),left,right);
					newExpr=(Expression) newExpr.accept(this);
					return new StmtAssign(stmt,stmt.getLHS(),newExpr);
				}
			}
		}*/
		return super.visitStmtAssign(stmt);
	}


	@Override
	public Object visitExprBinary(ExprBinary exp)
	{
		if(exp.getOp()==ExprBinary.BINOP_LSHIFT)
			exp=new ExprBinary(exp,ExprBinary.BINOP_LSHIFT,exp.getLeft(),exp.getRight());
		else if(exp.getOp()==ExprBinary.BINOP_RSHIFT)
			exp=new ExprBinary(exp,ExprBinary.BINOP_RSHIFT,exp.getLeft(),exp.getRight());
		return super.visitExprBinary(exp);
	}

	@Override
	public Object visitStmtVarDecl(StmtVarDecl stmt)
	{
		//convert all array initializations to separate assignments.
		//Unless they are constant expressions.
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

				Expression initExpr=stmt.getInit(i);
				if(false && initExpr==null && !name.startsWith("__"))
					initExpr=new ExprConstInt(stmt,0);
				if(initExpr!=null && !(initExpr instanceof ExprArrayInit)) {
					Expression init = null;
					Type type = stmt.getType(i);
					if( type instanceof TypeArray ){
						List<Expression> ilist = new ArrayList<Expression>();
						int N = ((TypeArray)type).getLength().getIValue();
						for(int s=0; s<N; ++s){
							ilist.add( ExprConstInt.zero );
						}
						init = new ExprArrayInit(stmt, ilist);
					}else{

						init = ExprConstInt.zero;
					}
					StmtVarDecl decl=new StmtVarDecl(stmt,stmt.getType(i),name,init);
					decl=(StmtVarDecl) super.visitStmtVarDecl(decl);
					statements.add(decl);
					StmtAssign let=new StmtAssign(new ExprVar(stmt,name),initExpr);
					statements.add((Statement)let.accept(this));
				}else{
					StmtVarDecl decl=new StmtVarDecl(stmt,stmt.getType(i),name,initExpr);
					decl=(StmtVarDecl) super.visitStmtVarDecl(decl);
					statements.add(decl);
				}
			}
			else {
				types.add(stmt.getType(i));
				names.add(stmt.getName(i));
				inits.add(stmt.getInit(i));
			}
		}
		if(n-na>0) {
			StmtVarDecl decl=new StmtVarDecl(stmt,types,names,inits);
			decl=(StmtVarDecl) super.visitStmtVarDecl(decl);
			addStatement(decl);
		}
		addStatements(statements);
		return null;
	}


	public Object visitExprFunCall(ExprFunCall exp)
    {
        boolean hasChanged = false;
        Function fun = nres.getFun(exp.getName(), exp);
        List formals = fun.getParams();
        Iterator form = formals.iterator();
        List<Expression> newParams = new ArrayList<Expression>();
        for (Iterator iter = exp.getParams().iterator(); iter.hasNext(); )
        {
            Expression param = (Expression)iter.next();
            Parameter formal = (Parameter) form.next();
            Expression newParam = doExpression(param);
            Type formalType = formal.getType();
            Type actualType = getType(newParam);
            if( !actualType.equals(formalType)  ){
            	assert ! formal.isParameterOutput() : "This should never happen";
            	newParam = makeTempExpr(newParam, formalType);
            }
            newParams.add(newParam);
            if (param != newParam) hasChanged = true;
        }
        if (!hasChanged) return exp;
        return new ExprFunCall(exp, exp.getName(), newParams);
    }


	@Override
	public Object visitPackage(Package spec)
	{
		starCheck=new HasStars(spec);
		return super.visitPackage(spec);
	}

	@Override
	public Object visitFunction(Function func)
	{
		if(func.getSpecification()==null && starCheck.testNode(func))
			return null;
		return super.visitFunction(func);
	}

}
