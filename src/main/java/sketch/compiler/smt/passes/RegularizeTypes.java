package sketch.compiler.smt.passes;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.SymbolTable;
import sketch.compiler.ast.core.exprs.*;
import sketch.compiler.ast.core.exprs.ExprArrayRange.RangeLen;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssert;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtIfThen;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.passes.lowering.SymbolTableVisitor;

/**
 * Some rules:
 * 
 * 0 and 1 are treated as bit, whenever an int is expected, cast 0 and 1 into an int
 * 
 * @author Lexin Shan
 * @email lshan@eecs.berkeley.edu
 *
 */
public class RegularizeTypes extends SymbolTableVisitor {

	private Type desiredType = null;

	private enum EXP_TYPE {
		BOOL,
		BIT,
		INT,
		VOID,
	};
	
	public RegularizeTypes() {
		super(null);
	}
	
	
	
	@Override
	public Object visitStmtAssign(StmtAssign stmt) {
		
		Expression left = stmt.getLHS();
		Expression right = stmt.getRHS();
		
		Type leftType = getType(left);
		
		desiredType = leftType;
		
		Expression newRight = right;
		StmtAssign newStmt = stmt;
		
		newRight = (Expression) right.accept(this);
		
		if (right != newRight)
			newStmt = new StmtAssign(stmt, left, newRight);
		
		return newStmt;
	}

	
	@Override
	public Object visitExprBinary(ExprBinary exp) {
		Type oldDesiredType = this.desiredType;
		
		Expression left = exp.getLeft();
		Expression right = exp.getRight();
		
		Type leftType = getType(left);
		Type rightType = getType(right);
		
		Type targetType;
		if (exp.getOp() == ExprBinary.BINOP_AND ||
				exp.getOp() == ExprBinary.BINOP_OR) {
			// if we expect bool on both sides, cast them to bool
			targetType = TypePrimitive.booltype;
		} else if (exp.getOp() == ExprBinary.BINOP_BAND || 
				exp.getOp() == ExprBinary.BINOP_BOR){
			// if we expect bit on both sides, cast them to bit
			targetType = TypePrimitive.bittype;
		} else {
			// promote to common type
			targetType = leftType.leastCommonPromotion(rightType);
		}
		
		this.desiredType = targetType; 
		Expression newLeft = (Expression) exp.getLeft().accept(this);
		this.desiredType = targetType;
		Expression newRight = (Expression) exp.getRight().accept(this);
		
		Expression newExpr = exp;
		
		if (left != newLeft || right != newRight)
			newExpr = new ExprBinary(exp, newLeft, exp.getOpString(), newRight);
	
		if (oldDesiredType != getType(newExpr))
			newExpr = new ExprTypeCast(newExpr, oldDesiredType, newExpr);
		
		return newExpr;
		
//ExprBinary binExpr = (ExprBinary) super.visitExprBinary(exp);
//		
//		Expression left = binExpr.getLeft();
//		Expression right = binExpr.getRight();
//		
//		Type leftType = getType(left);
//		Type rightType = getType(right);
//		
//		Expression newLeft = left, newRight = right, newExpr = binExpr;
//		Type desiredType = null;
//		
//		if (exp.getOp() == ExprBinary.BINOP_AND ||
//				exp.getOp() == ExprBinary.BINOP_OR) {
//			// if we expect bool on both sides, cast them to bool
//			desiredType = TypePrimitive.booltype;
//		} else {
//			// promote to common type
//			desiredType = leftType.leastCommonPromotion(rightType);
//		}
//		
//		if (!leftType.equals(desiredType))
//			newLeft = new ExprTypeCast(left, desiredType, left);
//		if (!rightType.equals(desiredType))
//			newRight = new ExprTypeCast(right, desiredType, right);
//		
//		if (left != newLeft || right != newRight)
//			newExpr = new ExprBinary(exp, newLeft, exp.getOpString(), newRight);
//	
//		return newExpr;
		
	}
	
	@Override
	public Object visitStmtIfThen(StmtIfThen stmtIfThen) {

		Expression cond = stmtIfThen.getCond();
		Statement thenBlk = stmtIfThen.getCons();
		Statement elseBlk = stmtIfThen.getAlt();
		
		desiredType = TypePrimitive.booltype;
		Expression newCond = (Expression) cond.accept(this);
		Statement newThenBlk = (Statement) thenBlk.accept(this);
		Statement newElseBlk = elseBlk != null ? (Statement) elseBlk.accept(this) : null;
		
		if (newCond != cond || newThenBlk != thenBlk || newElseBlk != elseBlk)
			return new StmtIfThen(stmtIfThen, newCond, newThenBlk, newElseBlk);
		
		return stmtIfThen;		
	}
	
	@Override
	public Object visitExprTernary(ExprTernary exp) {
		
		Type oldDesiredType = desiredType;
		
		// For expressions of the form a ? b :c
		// Make sure b's type and c's type are the same. If not, add casts
		Expression oldA = exp.getA();		
		Expression oldB = exp.getB();
		Expression oldC = exp.getC();
		
		desiredType = TypePrimitive.booltype;
		Expression newA = (Expression) oldA.accept(this);
		desiredType = oldDesiredType;
		Expression newB = (Expression) oldB.accept(this);
		desiredType = oldDesiredType;
		Expression newC = (Expression) oldC.accept(this);
		
		if (newA != oldA || newB != oldB || newC != oldC) {
			return new ExprTernary(exp, exp.getOp(), newA, newB, newC);
		}
		return exp;
	}
	
	@Override
	public Object visitStmtVarDecl(StmtVarDecl stmt)
    {
		
		List<Expression> newInits = new ArrayList<Expression>();
 
        boolean changed = false;
        for (int i = 0; i < stmt.getNumVars(); i++)
        {
        	symtab.registerVar(stmt.getName(i),
                    actualType(stmt.getType(i)),
                    stmt,
                    SymbolTable.KIND_LOCAL);
        	
        	Expression oinit = stmt.getInit(i);
        	Expression init = oinit;
        	
        	if (oinit != null) {
        		desiredType = stmt.getType(i);            
        		init = (Expression) oinit.accept(this);

	            if (oinit != init){
	            	changed = true;
	            }
            }
            newInits.add(init);
          
        }
        if(!changed){ return stmt; }
        return new StmtVarDecl(stmt, stmt.getTypes(),
                               stmt.getNames(), newInits);
    }
	
	@Override
	public Object visitExprConstBoolean(ExprConstBoolean exp) {
		if  (!desiredType.equals(TypePrimitive.booltype)) {
			return new ExprTypeCast(exp, desiredType, exp);
		}
		return exp;
	}
	
	@Override
	public Object visitExprConstInt(ExprConstInt exp) {
		if (!desiredType.equals(getType(exp))) {
			return new ExprTypeCast(exp, desiredType, exp);
		}
		return exp;
	}
	
	@Override
	public Object visitExprNullPtr(ExprNullPtr nptr) {
		return new ExprTypeCast(nptr, TypePrimitive.inttype, nptr);
	}

	@Override
	public Object visitTypeArray(TypeArray t) {
		return t;
	}
	
	@Override
	public Object visitExprVar(ExprVar exp) {
		
		Type exprType = getType(exp);
		
		if (exprType instanceof TypeArray) {
			// we never need to cast an array
			return exp;
		}
		if (!desiredType.equals(getType(exp))) {
			return new ExprTypeCast(exp, desiredType, exp);
		}
		return exp;
	}
	
	@Override
	public Object visitExprStar(ExprStar exp) {
		Type starType = getType(exp);
		if (starType instanceof TypeArray) {
			return exp;
		} else {
			return new ExprTypeCast(exp, desiredType, exp);
		}
	}
	
	@Override
	public Object visitExprArrayRange(ExprArrayRange exp) {
		
		boolean change=false;
		
		Type oldDesiredType = desiredType;
		
		Type exprType = getType(exp.getBase());
		TypeArray baseType = (TypeArray) exprType;
		desiredType = new TypeArray(oldDesiredType, baseType.getLength());
		
		// Do not attempt to add type cast to the base, because the base
		// can be an array.
		// for instance,
		// if (a[b]) // the base is bit[10], desired type is boolean,
		// casting bit[10] to boolean[10] doesn't make any sense
		final Expression newBase=doExpression(exp.getBase());
		if(newBase!=exp.getBase()) change=true;
		
		
		
		
        RangeLen range=exp.getSelection();
        desiredType = TypePrimitive.inttype;
        Expression newStart=doExpression(range.start());
        Expression newLen = doExpression(range.getLenExpression());
        if(newStart!=range.start() || newLen != range.getLenExpression()) {
            range=new RangeLen(newStart,newLen);
            change=true;
        }
    
        
		
		
		ExprArrayRange newRangeExpr = new ExprArrayRange(exp, newBase, range); 
		Type rangeExprType = getType(newRangeExpr);
		if (!rangeExprType.equals(oldDesiredType)) {
			return new ExprTypeCast(exp, oldDesiredType, newRangeExpr);
		}
		return newRangeExpr;
	}
	
	@Override
	public Object visitExprTypeCast(ExprTypeCast exp) {
		
		// this is kind of hacky
		if (exp.getType().equals(desiredType))
			return exp;
		else
			return exp.getExpr().accept(this); 
	}
	
	@Override
	public Object visitExprFunCall(ExprFunCall exp) {
		Type oldDesiredType = desiredType;
		boolean hasChanged = false;
        List<Expression> newParams = new ArrayList<Expression>();
        Function func = nres.getFun(exp.getName());
        Iterator<Parameter> paramIter = func.getParams().iterator();
        for (Iterator iter = exp.getParams().iterator(); iter.hasNext(); )
        {
            Expression param = (Expression)iter.next();
            Parameter formalParam = paramIter.next();
            
            desiredType = formalParam.getType();
            Expression newParam = doExpression(param);
            newParams.add(newParam);
            if (param != newParam) hasChanged = true;
        }
        Expression ret = exp;
        
        if (!hasChanged) return exp;
        
        ret = new ExprFunCall(exp, exp.getName(), newParams);
//        if (oldDesiredType != null && !oldDesiredType.equals(func.getReturnType()))
//        	ret = new ExprTypeCast(exp, oldDesiredType, ret);
        return ret;
	}
	
	@Override
	public Object visitStmtAssert(StmtAssert stmt) {
		this.desiredType = TypePrimitive.booltype;
		return super.visitStmtAssert(stmt);
	}
	
	@Override
	public Object visitExprArrayInit(ExprArrayInit exp) {
		desiredType = ((TypeArray) desiredType).getBase();
		return super.visitExprArrayInit(exp);
	}
	
	
	@Override
	public Type getType(Expression expr) {
		return super.getType(expr);
	}

	

	/*
	 * For Debugging purpose
	 */
	
	

	
}
