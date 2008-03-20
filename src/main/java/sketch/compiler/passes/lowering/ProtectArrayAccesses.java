package streamit.frontend.passes;

import java.util.ArrayList;
import java.util.List;

import streamit.frontend.nodes.ExprArrayRange;
import streamit.frontend.nodes.ExprBinary;
import streamit.frontend.nodes.ExprConstInt;
import streamit.frontend.nodes.ExprTernary;
import streamit.frontend.nodes.ExprUnary;
import streamit.frontend.nodes.ExprVar;
import streamit.frontend.nodes.Expression;
import streamit.frontend.nodes.FEReplacer;
import streamit.frontend.nodes.Statement;
import streamit.frontend.nodes.StmtAssert;
import streamit.frontend.nodes.StmtAssign;
import streamit.frontend.nodes.StmtBlock;
import streamit.frontend.nodes.StmtIfThen;
import streamit.frontend.nodes.StmtVarDecl;
import streamit.frontend.nodes.TempVarGen;
import streamit.frontend.nodes.TypeArray;
import streamit.frontend.nodes.TypePrimitive;


/**
 * The purpose of this class is to replace right-hand-side array
 * accesses of the form A[x] into expressions of the form:
 * (x>=0 && x < N) ? A[x] : 0;
 *
 * Similarly, Left hand side A[x] gets replaced with
 *
 * if(x>=0 && x < N){ A[x] = rhs; }
 *
 * Assumes that conditional expressions have been eliminated.
 *
 * @author asolar
 *
 */
public class ProtectArrayAccesses extends SymbolTableVisitor {
	/** What happens when an access is out of bounds? */
	public static enum FailurePolicy { ASSERTION, WRSILENT_RDZERO };

	private TempVarGen varGen;
	private FailurePolicy policy;

	public ProtectArrayAccesses(TempVarGen vargen){
		this (FailurePolicy.WRSILENT_RDZERO, vargen);
	}

	public ProtectArrayAccesses (FailurePolicy p, TempVarGen vargen) {
		super(null);
		this.varGen = vargen;
		this.policy = p;
	}

	/** We have to conditionally protect array accesses for shortcut operators. */
	public Object visitExprBinary (ExprBinary eb) {
		String op = eb.getOpString ();
		if (op.equals ("&&"))
			return doLogicalExpr (eb, true);
		else if (op.equals ("||"))
			return doLogicalExpr (eb, false);
		else
			return super.visitExprBinary (eb);
	}

	protected Expression doLogicalExpr (ExprBinary eb, boolean isAnd) {
		Expression left = eb.getLeft (), right = eb.getRight ();

		if (!(hasArrayAccess (left) || hasArrayAccess (right)))
			return eb;

		left = doExpression (left);

		String resName = varGen.nextVar ("_pac_sc");
		addStatement (new StmtVarDecl (eb, TypePrimitive.booltype, resName, left));
		ExprVar res = new ExprVar (eb, resName);

		List<Statement> oldStatements = newStatements;
        newStatements = new ArrayList<Statement>();

        right = doExpression (right);
        newStatements.add (new StmtAssign (res, right));

        StmtBlock thenBlock = new StmtBlock (eb, newStatements);
        newStatements = oldStatements;

        // What is the condition on 'res' that causes us to fully evaluate
        // the expression?  If it's a logical AND, and 'res' is true, then we
        // need to evaluate the right expr.  If it's a logical OR, and 'res' is
        // false, then we need to evaluate the right expr.
        Expression cond = isAnd ? res : new ExprUnary ("!", res);

        addStatement (new StmtIfThen (eb, cond, thenBlock, null));

        return res;
	}

	@Override
	public Object visitExprArrayRange(ExprArrayRange ear){
		assert ear.hasSingleIndex() : "Array ranges not allowed in parallel code.";

		Expression idx = makeLocalIndex (ear);
		Expression base = (Expression) ear.getBase ().accept (this);
		Expression cond = makeGuard (base, idx);
		Expression near = new ExprArrayRange(base, idx);

		if (FailurePolicy.WRSILENT_RDZERO == policy) {
			return new ExprTernary("?:", cond, near, ExprConstInt.zero);
		} else if (FailurePolicy.ASSERTION == policy){
			addStatement (new StmtAssert (cond));
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
			Expression cond = makeGuard (base, idx);
			Expression near = new ExprArrayRange(base, idx);
			Expression rhs = (Expression) stmt.getRHS ().accept (this);

			if (FailurePolicy.WRSILENT_RDZERO == policy) {
				return new StmtIfThen(stmt, cond, new StmtAssign(near, rhs), null);
			} else if (FailurePolicy.ASSERTION == policy) {
				addStatement (new StmtAssert (cond));
				return new StmtAssign (near, rhs);
			} else {  assert false : "fatal error"; return null;  }
		}else{
			return super.visitStmtAssign(stmt);
		}
    }

	protected Expression makeLocalIndex (ExprArrayRange ear) {
		String nname = varGen.nextVar("_pac");
		Expression nofset = (Expression) ear.getOffset().accept(this);
		addStatement(new StmtVarDecl(ear, TypePrimitive.inttype, nname,  nofset));
		return new ExprVar(ear, nname);
	}

	protected Expression makeGuard (Expression base, Expression idx) {
		Expression sz = ((TypeArray)getType(base)).getLength();
		return new ExprBinary(new ExprBinary(idx, ">=", ExprConstInt.zero), "&&",
										 new ExprBinary(idx, "<", sz));
	}

	protected boolean hasArrayAccess (Expression e) {
		class checker extends FEReplacer {
    		public Object visitExprArrayRange (ExprArrayRange ear) {
    			throw new RuntimeException ("yes");
    		}
    	};
    	try {
    		e.accept (new checker ());
    	} catch (RuntimeException re) {
    		if ("yes".equals (re.getMessage ()))
    			return true;
    		throw re;
    	}
    	return false;
	}
}
