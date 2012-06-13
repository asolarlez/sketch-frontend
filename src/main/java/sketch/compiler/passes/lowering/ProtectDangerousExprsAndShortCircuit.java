package sketch.compiler.passes.lowering;
import java.util.ArrayList;
import java.util.List;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.ExprArrayRange;
import sketch.compiler.ast.core.exprs.ExprArrayRange.RangeLen;
import sketch.compiler.ast.core.exprs.ExprBinary;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprConstant;
import sketch.compiler.ast.core.exprs.ExprTernary;
import sketch.compiler.ast.core.exprs.ExprUnary;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssert;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtAtomicBlock;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.stmts.StmtIfThen;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.ast.core.typs.TypeStruct;
import sketch.compiler.ast.core.typs.TypeStructRef;
import sketch.util.ControlFlowException;

/**
 * Protect dangerous expressions such as array accesses, division and dereferences. Also
 * enforces short circuit behavior for && and ||. Single | and & should not short circuit.
 * 
 * @author asolar
 */
public class ProtectDangerousExprsAndShortCircuit extends SymbolTableVisitor {
	/** What happens when an access is out of bounds? */
	public static enum FailurePolicy { ASSERTION, WRSILENT_RDZERO };

	private TempVarGen varGen;
	private FailurePolicy policy;

	public ProtectDangerousExprsAndShortCircuit(TempVarGen vargen){
		this (FailurePolicy.WRSILENT_RDZERO, vargen);
	}

	public ProtectDangerousExprsAndShortCircuit (FailurePolicy p, TempVarGen vargen) {
		super(null);
		this.varGen = vargen;
		this.policy = p;
	}

//        @Override
//        public Object visitTypeArray(TypeArray arr) {
//            // FIXME: for array access in parameter (varlength array, for example), we cannot generate protection properly 
//            if (newStatements == null) {
//                return arr;
//            }
//            return super.visitTypeArray(arr);
//        }

	/** We have to conditionally protect array accesses for shortcut operators. */
	public Object visitExprBinary (ExprBinary eb) {
                // FIXME: for array access in parameter (varlength array, for example), we cannot generate protection properly 
                //assert(newStatements != null);
		String op = eb.getOpString ();
        if (op.equals("&&") || op.equals("||"))
			return doLogicalExpr (eb);
		else {
            if (op.equals("/") || op.equals("%")) {
                addStatement(new StmtAssert(new ExprBinary(eb.getRight(), "!=",
                        ExprConstInt.zero), eb.getCx() + ": Division by zero", false));
            }
            return super.visitExprBinary(eb);

        }
	}
	
	
	public Object visitExprTernary(ExprTernary exp){
		Expression a = exp.getA().doExpr(this);
		Expression b = exp.getB();
		Expression c = exp.getC();
		
		
		if (!(hasArrayAccess (b) || hasArrayAccess (c))){			
			if (a == exp.getA() && b == exp.getB() && c == exp.getC())
	            return exp;
	        else
	            return new ExprTernary(exp, exp.getOp(), a, b, c);
		}
			
		
		
		String resName = varGen.nextVar ("_pac_sc");
		Type t = getTypeReal(exp);
		if(t == TypePrimitive.nulltype){
			t = TypePrimitive.inttype;
		}
		addStatement (new StmtVarDecl (exp,t, resName, null));
		ExprVar res = new ExprVar (exp, resName);

		
		StmtBlock thenBlock = null;
		{
			List<Statement> oldStatements = newStatements;
	        newStatements = new ArrayList<Statement>();
	
	        b = doExpression (b);
	        newStatements.add (new StmtAssign (res, b));
	
	        thenBlock = new StmtBlock (exp, newStatements);
	        newStatements = oldStatements;
		}
		
		StmtBlock elseBlock = null;
		if(c != null){
			List<Statement> oldStatements = newStatements;
	        newStatements = new ArrayList<Statement>();
	
	        c = doExpression (c);
	        newStatements.add (new StmtAssign (res, c));
	
	        elseBlock = new StmtBlock (exp, newStatements);
	        newStatements = oldStatements;
		}
		
        // What is the condition on 'res' that causes us to fully evaluate
        // the expression?  If it's a logical AND, and 'res' is true, then we
        // need to evaluate the right expr.  If it's a logical OR, and 'res' is
        // false, then we need to evaluate the right expr.
        

        addStatement (new StmtIfThen (exp, a, thenBlock, elseBlock));		
		
		 
		return res;
	}
	

	protected Expression doLogicalExpr (ExprBinary eb) {
		Expression left = eb.getLeft (), right = eb.getRight ();

		if (!(hasArrayAccess (left) || hasArrayAccess (right)))
			return eb;

		boolean isAnd = eb.getOpString ().equals ("&&") || eb.getOpString ().equals ("&");

		
		String resName = varGen.nextVar ("_pac_sc");
		
		addStatement(new StmtVarDecl (eb, TypePrimitive.bittype, resName, null));
		
		ExprVar res = new ExprVar (eb, resName);
		Expression cond = isAnd ? res : new ExprUnary ("!", res);
		List<Statement> blist = new ArrayList<Statement>();
		blist.add (new StmtAssign (res, right));
		StmtBlock nb = 
			new StmtBlock(
					new StmtAssign (res, left), 			
					new StmtIfThen (eb, cond, new StmtBlock(blist), null)
			);

		doStatement(nb);
		
        return res;
	}

	@Override
	public Object visitExprArrayRange(ExprArrayRange ear){		
                // FIXME: for array access in parameter (varlength array, for example), we cannot generate protection properly 
                assert(newStatements != null);

		Expression idx = makeLocalIndex (ear);
		Expression base = (Expression) ear.getBase ().accept (this);
		
		Expression cond = null;
		Expression near = null;
		
		if(ear.hasSingleIndex()){		
			cond = makeGuard (base, idx);		
			near = new ExprArrayRange(base, idx);
		}else{
			RangeLen rl = ear.getSelection();
			Expression ofst = (Expression) rl.getLenExpression().accept(this);
			cond = makeGuard (base, ofst  ,idx);		
			near = new ExprArrayRange(ear, base, new RangeLen(idx, ofst));
		}

        if (cond == null) {
            return near;
        }
		if (FailurePolicy.WRSILENT_RDZERO == policy) {
			return new ExprTernary("?:", cond, near, ExprConstInt.zero);
        } else if (FailurePolicy.ASSERTION == policy) {
            addStatement(new StmtAssert(cond, cond.getCx() + ": Array out of bounds",
                    false));
			return near;
		} else {  assert false : "fatal error"; return null;  }
	}

	@Override
    public Object visitStmtAssign(StmtAssign stmt)
    {
		if(stmt.getLHS() instanceof ExprArrayRange){
			ExprArrayRange ear = (ExprArrayRange) stmt.getLHS();
			Expression idx = makeLocalIndex (ear);
			Expression base = (Expression) ear.getBase ().accept (this);
			
			Expression cond = null;
			Expression near = null;
			
			if(ear.hasSingleIndex()){		
				cond = makeGuard (base, idx);		
				near = new ExprArrayRange(base, idx);
			}else{
				RangeLen rl = ear.getSelection();
				Expression ofst = (Expression) rl.getLenExpression().accept(this);
				cond = makeGuard (base, ofst  ,idx);		
				near = new ExprArrayRange(ear, base, new RangeLen(idx, ofst));
			}
			
			
			Expression rhs = (Expression) stmt.getRHS ().accept (this);
			int op = stmt.getOp();

			if (FailurePolicy.WRSILENT_RDZERO == policy) {
                if (cond != null) {
                    return new StmtIfThen(stmt, cond, new StmtAssign(near, rhs, op), null);
                } else {
                    return new StmtAssign(near, rhs, op);
                }
			} else if (FailurePolicy.ASSERTION == policy) {
                if (cond != null) {
                    addStatement(new StmtAssert(ear, cond, false));
                }
				return new StmtAssign (stmt, near, rhs, op);
			} else {  assert false : "fatal error"; return null;  }
		}else{
			return super.visitStmtAssign(stmt);
		}
    }

	@Override
	public Object visitStmtAtomicBlock (StmtAtomicBlock sab) {
		if (!sab.isCond () || FailurePolicy.WRSILENT_RDZERO == policy)
			return super.visitStmtAtomicBlock (sab);

		// This rewrite is a bit hairier than the one for the other statement
		// types.  See bug 55 in bugzilla.

		CollectAtomicCondGuards acg = new CollectAtomicCondGuards ();
		Expression newCond = acg.makeAtomicCond (sab.getCond ());
		List<Statement> newBody = new ArrayList<Statement> (acg.getInnerAssertions ());
		StmtBlock newBlock = (StmtBlock) sab.getBlock ().accept (this);
		newBody.addAll (newBlock.getStmts ());

		return new StmtAtomicBlock (sab, newBody, newCond);
	}

	private class CollectAtomicCondGuards extends FEReplacer {
		/** A list of "predicated" assertions to check within the body of the
		 * atomic, to ensure that no null pointers were dereferenced
		 * while evaluating the atomic's condition. */
		private List<StmtAssert> guardStmts = new ArrayList <StmtAssert> ();
		/** An expression to "trap" errors; if this expression is true, then
		 * one of the assertions in 'guardStmts' must fail. */
		private Expression condTrap;
		// A list rather than a stack because we need to traverse it
		// front to back
		private List<Expression> guards = new ArrayList<Expression> ();

		public Expression makeAtomicCond (Expression cond) {
			cond.accept (this);
			return (condTrap == null) ? cond
					: new ExprBinary (condTrap, "||", cond);
		}

		public List<StmtAssert> getInnerAssertions () {
			return guardStmts;
		}

		/**
		 * Creates an assertion to check the following condition:
		 *
		 *   conds-to-eval-array-access => guard
		 *
		 * which must be rewritten to:
		 *
		 *   !conds-to-eval-array-access || guard
		 */
		private void addGuardAssertion (Expression guard) {
			Expression cond = ExprConstant.createConstant (guard, "1");

			for (Expression g : guards)
				cond = new ExprBinary (cond, "&&", g);

			cond = new ExprUnary ("!", cond);
			cond = new ExprBinary (cond, "||", guard);

			guardStmts.add (new StmtAssert (cond, "out-of-bounds array access", false));

			Expression trap = new ExprUnary ("!", cond);
			if (condTrap == null)
				condTrap = trap;
			else
				condTrap = new ExprBinary (condTrap, "||", trap);
		}

		private void pushGuard (Expression guard) { guards.add (guard); }
		private Expression popGuard () { return guards.remove (guards.size () - 1); }

		public Object visitExprArrayRange (ExprArrayRange exp) {
			assert exp.getArrayIndices ().size () == 1;

			Expression base = doExpression (exp.getBase ());
			Expression index = doExpression (exp.getArrayIndices ().get (0));
			Expression guard = makeGuard (base, index);

			addGuardAssertion (guard);
			return exp;
		}

		// Short-circuit evaluation places guards on expr eval
		public Object visitExprBinary (ExprBinary exp) {
			String op = exp.getOpString ();
			Expression left = doExpression (exp.getLeft ());

			if (op.equals ("&&")) {
				pushGuard (new ExprUnary ("!", left));
				doExpression (exp.getRight ());
				popGuard ();
			} else if (op.equals ("||")) {
				pushGuard (left);
				doExpression (exp.getRight ());
				popGuard ();
			} else
				doExpression (exp.getRight ());

			return exp;
		}

		// Conditional expressions place guards on expr eval
		public Object visitExprTernary (ExprTernary exp) {
			Expression A = doExpression (exp.getA ());
			pushGuard (A);
			doExpression (exp.getB ());
			pushGuard (new ExprUnary ("!", popGuard ()));
			doExpression (exp.getC ());
			popGuard ();
			return exp;
		}
	}

	protected Expression makeLocalIndex (ExprArrayRange ear) {
                // FIXME: for array access in parameter (varlength array, for example), we cannot generate protection properly 
                assert (newStatements != null);
		String nname = varGen.nextVar("_pac");				
		RangeLen rl =ear.getSelection(); 
		Expression nofset = (Expression) rl.start()/*.accept(this)*/;
		Type t = getType(nofset);
		addStatement((Statement)(new StmtVarDecl(ear, t, nname,  nofset).accept(this)));
		return new ExprVar(ear, nname);
	}

	
	protected Expression makeGuard (Expression base, Expression idx) {
		Type idxt = getType(idx);
		Expression sz = ((TypeArray) getType(base)).getLength();
        if (sz == null && (idxt instanceof TypeStruct || idxt instanceof TypeStructRef)) {
            return new ExprBinary(idx, "!=", ExprConstInt.minusone);
        }
        if (sz == null) {
            return new ExprBinary(idx, ">=", ExprConstInt.zero);
        }
		if(idxt instanceof TypeStruct || idxt instanceof TypeStructRef){
			return new ExprBinary(new ExprBinary(idx, "!=", ExprConstInt.minusone), "&&",
										 new ExprBinary(idx, "<", sz));
		}else{
			return new ExprBinary(new ExprBinary(idx, ">=", ExprConstInt.zero), "&&",
					 new ExprBinary(idx, "<", sz));			
		}
	}
	
	protected Expression makeGuard (Expression base, Expression len, Expression idx) {
		Expression sz = ((TypeArray) getType(base)).getLength();
        Expression ex = new ExprBinary(idx, ">=", ExprConstInt.zero);
        if (sz == null) {
            return ex;
        }
        return new ExprBinary(ex, "&&", new ExprBinary(new ExprBinary(idx, "+", len),
                "<=", sz));
	}


	protected boolean hasArrayAccess (Expression e) {
		class checker extends FEReplacer {
    		public Object visitExprArrayRange (ExprArrayRange ear) {
    			throw new ControlFlowException ("yes");
    		}

            public Object visitExprBinary(ExprBinary eb) {
                if (eb.getOp() == ExprBinary.BINOP_DIV ||
                        eb.getOp() == ExprBinary.BINOP_MOD)
                {
                    throw new ControlFlowException("yes");
                }
                return super.visitExprBinary(eb);
            }
    	};
    	try {  e.accept (new checker ());   return false;  }
    	catch (ControlFlowException cfe) {  return true;  }
	}
}
