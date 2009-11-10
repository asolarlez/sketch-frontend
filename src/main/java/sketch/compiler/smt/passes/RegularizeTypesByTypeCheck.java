package sketch.compiler.smt.passes;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.SymbolTable;
import sketch.compiler.ast.core.exprs.ExprArrayInit;
import sketch.compiler.ast.core.exprs.ExprArrayRange;
import sketch.compiler.ast.core.exprs.ExprBinary;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprNullPtr;
import sketch.compiler.ast.core.exprs.ExprTernary;
import sketch.compiler.ast.core.exprs.ExprTypeCast;
import sketch.compiler.ast.core.exprs.ExprUnary;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.exprs.ExprArrayRange.Range;
import sketch.compiler.ast.core.exprs.ExprArrayRange.RangeLen;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssert;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtIfThen;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.stmts.StmtWhile;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.ast.core.typs.TypeStruct;
import sketch.compiler.passes.lowering.SymbolTableVisitor;
import sketch.compiler.smt.partialeval.BitVectUtil;

public class RegularizeTypesByTypeCheck extends SymbolTableVisitor {

	public RegularizeTypesByTypeCheck() {
		super(null);
	}

	// Array related stuff
	@Override
	public Object visitExprArrayRange(ExprArrayRange exp) {
		// TODO make sure the arary index is int
		// if not, cast it to int
		
		Expression base = (Expression) exp.getBase().accept(this);

		List members = new LinkedList();
		boolean changed = false;
		for (Object o : exp.getMembers()) {
			Object newO = null;
			if (o instanceof Range) {
				newO = visitRange((Range) o);
			} else if (o instanceof RangeLen) {
				newO = visitRangeLen((RangeLen) o);
			}
			if (o != newO) changed = true;
			members.add(newO);
		}
		
		if (changed 
				|| base != exp.getBase())
			exp = new ExprArrayRange(exp, base, members);
		return exp;
	}
	
	protected Range visitRange(Range r) {
		Expression start = (Expression) r.start().accept(this);
		Expression end = (Expression) r.end().accept(this);
		
		start = castIfTypeIncorrect(TypePrimitive.inttype, start);
		end = castIfTypeIncorrect(TypePrimitive.inttype, end);
		
		if (start != r.start() || end != r.end())
			r = new Range(start, end);
		return r;
	}
	
	protected RangeLen visitRangeLen(RangeLen rl) {
		
		Expression start = (Expression) rl.start().accept(this);
		start = castIfTypeIncorrect(TypePrimitive.inttype, start);
		
		
		Expression len = rl.getLenExpression();
		if (rl.hasLenExpression()) {
			len = (Expression) len.accept(this);
			len = castIfTypeIncorrect(TypePrimitive.inttype, len);
			if (start != rl.start() || len != rl.getLenExpression())
				rl = new RangeLen(start, len);
		} else {
			if (start != rl.start())
				rl = new RangeLen(start, rl.len());
		}
		
		
		return rl;
	}
	
	@Override
	public Object visitExprArrayInit(ExprArrayInit exp) {
	
		// first determine the common type for all the init exprs
		Type commonType = null;
		for (Expression init : exp.getElements()) {
			if (commonType == null)
				commonType = getType(init);
			
			Type initType = getType(init);
			if (!commonType.equals(initType)) {
				commonType = commonType.leastCommonPromotion(initType);
			}
		}
		boolean changed = false;
		List<Expression> initExprs = new LinkedList<Expression>();
		for (Expression init : exp.getElements()) {
			Expression newInit = castIfTypeIncorrect(commonType, init);
			if (init != newInit)
				changed = true;
	
			initExprs.add(newInit);
		}
		
		if (changed)
			exp = new ExprArrayInit(exp, initExprs);
		
		return exp;
	}
	
	// Statement related stuff
	@Override
	public Object visitStmtVarDecl(StmtVarDecl stmt) {
		// TODO make sure the init expression is the declared type
		List<Expression> newInits = new ArrayList<Expression>();
		 
        boolean changed = false;
        for (int i = 0; i < stmt.getNumVars(); i++)
        {
        	symtab.registerVar(stmt.getName(i),
                    actualType(stmt.getType(i)),
                    stmt,
                    SymbolTable.KIND_LOCAL);
        	
        	Expression init = stmt.getInit(i);
        	
        	if (init != null) {
        		Type desiredType = stmt.getType(i);            
        		init = (Expression) init.accept(this);
        		
        		init = castIfTypeIncorrect(desiredType, init);
        		
	            if (stmt.getInit(i) != init){
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
	public Object visitStmtAssert(StmtAssert stmt) {
		// TODO make sure the condition is bool
		Expression cond = (Expression) stmt.getCond().accept(this);
		
		cond = castIfTypeIncorrect(TypePrimitive.booltype, cond);
		
		if (cond != stmt.getCond())
			stmt = new StmtAssert(stmt, cond, stmt.getMsg(), stmt.isSuper());
		return stmt;
	}
	
	@Override
	public Object visitStmtAssign(StmtAssign stmt) {
		// TODO make sure the RHS matches LHS
		
		Expression lhs = (Expression) stmt.getLHS().accept(this);
		Expression rhs = (Expression) stmt.getRHS().accept(this);
		Type leftType = getType(lhs);
		
		rhs = castIfTypeIncorrect(leftType, rhs);
		
		if (rhs != stmt.getRHS() || lhs != stmt.getLHS())
			stmt = new StmtAssign(stmt, lhs, rhs);
	
		return stmt;
	}
	
	@Override
	public Object visitStmtIfThen(StmtIfThen stmtIfThen) {
		// make sure the condition is bool

		Expression cond = (Expression) stmtIfThen.getCond().accept(this);
		Statement cons = (Statement) stmtIfThen.getCons().accept(this);
		Statement alt = (stmtIfThen.getAlt() != null) ? 
				(Statement) stmtIfThen.getAlt().accept(this) : null;
		
		cond = castIfTypeIncorrect(TypePrimitive.booltype, cond);
		
		if (stmtIfThen.getCond() != cond 
				|| stmtIfThen.getCons() != cons 
				|| stmtIfThen.getAlt() != alt)
			return new StmtIfThen(stmtIfThen, cond, cons, alt);
		
		return stmtIfThen;	
	}
	
	@Override
	public Object visitStmtWhile(StmtWhile stmt) {
		// make sure the condition is bool
		Expression cond = (Expression) stmt.getCond().accept(this);
		
		cond = castIfTypeIncorrect(TypePrimitive.booltype, cond);
		Statement body = (Statement) stmt.getBody().accept(this);
		
		if (cond != stmt.getCond() ||
				body != stmt.getBody())
			return new StmtWhile(stmt, cond, body);
		
	
		return stmt;
	}
	
	// Expression related stuff
	@Override
	public Object visitExprTernary(ExprTernary exp) {
		// TODO make sure the condition is bool
		
		Expression a = (Expression) exp.getA().accept(this);
		Expression b = (Expression) exp.getB().accept(this);
		Expression c = (Expression) exp.getC().accept(this);
		
		a = castIfTypeIncorrect(TypePrimitive.booltype, a);
		
		Type bType = getType(b);
		Type cType = getType(c);
		
		if (!cType.equals(bType)) {
			Type commonType = cType.leastCommonPromotion(bType);
			
			b = castIfTypeIncorrect(commonType, b);
			c = castIfTypeIncorrect(commonType, c);
		}
		
		if (a != exp.getA() || b != exp.getB() || c != exp.getC()) {
			return new ExprTernary(exp, exp.getOp(), a, b, c);
		}
		return exp;
	}
	
	@Override
	public Object visitExprUnary(ExprUnary exp) {
	
		Expression sub = (Expression) exp.getExpr().accept(this);
		// bitwise ~
		if (exp.getOp() == ExprUnary.UNOP_BNOT) {
			sub = castIfTypeMatchesIncorrect(TypePrimitive.booltype, TypePrimitive.bittype, sub);
		}
		
		// logical not
		if (exp.getOp() == ExprUnary.UNOP_NOT) {
			sub = castIfTypeIsNotInSet(sub, TypePrimitive.booltype, true, TypePrimitive.booltype, TypePrimitive.bittype);
		}
		
		if (sub != exp.getExpr())
			exp = new ExprUnary(exp, exp.getOp(), sub);
		return exp;
		
	}
	
	@Override
	public Object visitExprFunCall(ExprFunCall exp) {
		Function f = sspec.getFuncNamed(exp.getName());
		
		Iterator<Parameter> paramIter = f.getParams().iterator();
		List<Expression> args = new LinkedList<Expression>();
		boolean changed = false;
		for (Expression arg : exp.getParams()) {
			Parameter p  = paramIter.next();
			Type paramType = p.getType();
			
			Expression newArg = arg;
			// do not to type casting if the it's a out param
			if (!p.isParameterOutput())
				newArg = castIfTypeIncorrect(paramType, arg);			
			if (newArg != arg) changed = true;

			args.add(newArg);
		}
		
		if (changed)
			exp = new ExprFunCall(exp, exp.getName(), args);
		return exp;
	}
	
	@Override
	public Object visitExprBinary(ExprBinary exp) {
		// TODO make sure LHS and RHS are of consistent type
		// cast it if not
		
		// + - > < >= < <=: bit and int, but convert to larger type first
		// == !=: all types
		// * /: int only
		
		
		// << >> : RHS: int LHS:int
 		Expression leftExpr = (Expression) exp.getLeft().accept(this);
		Expression rightExpr = (Expression) exp.getRight().accept(this);
		Type leftType = getType(leftExpr);
		Type rightType = getType(rightExpr);
		Type commonType = getLeastCommonType(leftType, rightType);
		
		// == and one hand side is null, no type cast needed.
		if (exp.getOp() == ExprBinary.BINOP_EQ && 
				(leftExpr instanceof ExprNullPtr || 
						rightExpr instanceof ExprNullPtr)) {
			return exp;
		}
		// & | ^ !: bit and int
		else if (exp.getOp() == ExprBinary.BINOP_BAND ||
				exp.getOp() == ExprBinary.BINOP_BOR ||
				exp.getOp() == ExprBinary.BINOP_BXOR) {
			leftExpr = castIfTypeMatchesIncorrect(TypePrimitive.booltype, TypePrimitive.bittype, leftExpr);
			rightExpr = castIfTypeMatchesIncorrect(TypePrimitive.booltype, TypePrimitive.bittype, rightExpr);
			leftExpr = castIfTypeIncorrect(commonType, leftExpr);
			rightExpr = castIfTypeIncorrect(commonType, rightExpr);	
		}
		// && || ^ !: boolean
		else if (exp.getOp() == ExprBinary.BINOP_AND ||
				exp.getOp() == ExprBinary.BINOP_OR) {
			leftExpr = castIfTypeIncorrect(TypePrimitive.booltype, leftExpr);
			rightExpr = castIfTypeIncorrect(TypePrimitive.booltype, rightExpr);
		
		} else if (exp.getOp() == ExprBinary.BINOP_RSHIFT || 
				exp.getOp() == ExprBinary.BINOP_LSHIFT) {
			rightExpr = castIfTypeIncorrect(TypePrimitive.inttype, rightExpr);
				
		} else if (exp.getOp() == ExprBinary.BINOP_GT ||
				exp.getOp() == ExprBinary.BINOP_GE ||
				exp.getOp() == ExprBinary.BINOP_LT ||
				exp.getOp() == ExprBinary.BINOP_LE) {
			leftExpr = castIfTypeIncorrect(TypePrimitive.inttype, leftExpr);
			rightExpr = castIfTypeIncorrect(TypePrimitive.inttype, rightExpr);
		} else {
			leftExpr = castIfTypeIncorrect(commonType, leftExpr);
			rightExpr = castIfTypeIncorrect(commonType, rightExpr);	
		}
			
		if (leftExpr != exp.getLeft() 
				|| rightExpr != exp.getRight()) {
			return new ExprBinary(exp, leftExpr, exp.getOpString(), rightExpr);
		}
		return exp;
	}
	
	

	/*
	 * Helpers
	 */
	
	Type getLeastCommonType(Type left, Type right) {
		if (left instanceof TypeStruct) {
			left = TypePrimitive.inttype;
		}
		
		if (right instanceof TypeStruct) {
			right = TypePrimitive.inttype;
		}
		
		if (BitVectUtil.isBitArray(left) && !BitVectUtil.isBitArray(right)) {
			return left;
		} else if (BitVectUtil.isBitArray(right) && !BitVectUtil.isBitArray(left)) {
			return right;
		} else if (BitVectUtil.isBitArray(left) && BitVectUtil.isBitArray(right)) {
			int leftSize = BitVectUtil.vectSize(left);
			int rightSize = BitVectUtil.vectSize(right);
			return leftSize > rightSize ? left : right;
		}
		
		return left.leastCommonPromotion(right);
	}
	Expression castIfTypeIncorrect(Type correctType, Expression exp) {
		assert correctType != null : "correctType in RegularizeTypesByTypeCheck be null";
		Type t = getType(exp);
		if (!t.equals(correctType))
			return new ExprTypeCast(exp, correctType, exp);
		return exp;
	}
	
	Expression castIfTypeIsNotInSet(Expression exp, Type correctType, boolean acceptBitArray, Type...acceptableTypes) {
		boolean isAcceptable = false;
		Type exprType = getType(exp);
		if (acceptBitArray && BitVectUtil.isBitArray(exprType))
			return exp;
		for (Type t : acceptableTypes) {
			if (exprType.equals(t)) {
				return exp;
			}
		}
		return new ExprTypeCast(exp, correctType, exp);
	}
	
	Expression castIfTypeMatchesIncorrect(Type incorrectType, Type correctType, Expression exp) {
		assert correctType != null : "correctType in RegularizeTypesByTypeCheck be null";
		assert incorrectType != null : "incorrectType in RegularizeTypesByTypeCheck be null";
		if (getType(exp).equals(incorrectType))
			return new ExprTypeCast(exp, correctType, exp);
		return exp;
	}
	
}
