package streamit.frontend.passes;

import streamit.frontend.nodes.ExprArrayRange;
import streamit.frontend.nodes.ExprBinary;
import streamit.frontend.nodes.ExprConstInt;
import streamit.frontend.nodes.ExprTernary;
import streamit.frontend.nodes.ExprVar;
import streamit.frontend.nodes.Expression;
import streamit.frontend.nodes.StmtAssert;
import streamit.frontend.nodes.StmtAssign;
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
 * @author asolar
 *
 */
public class ProtectArrayAccesses extends SymbolTableVisitor {
	/** What happens when an access is out of bounds? */
	public static enum FailurePolicy { ASSERTION, WRSILENT_RDZERO };

	private TempVarGen vargen;
	private FailurePolicy policy;

	public ProtectArrayAccesses(TempVarGen vargen){
		this (FailurePolicy.WRSILENT_RDZERO, vargen);
	}

	public ProtectArrayAccesses (FailurePolicy p, TempVarGen vargen) {
		super(null);
		this.vargen = vargen;
		this.policy = p;
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
		String nname = vargen.nextVar("_i");
		Expression nofset = (Expression) ear.getOffset().accept(this);
		addStatement(new StmtVarDecl(ear, TypePrimitive.inttype, nname,  nofset));
		return new ExprVar(ear, nname);
	}

	protected Expression makeGuard (Expression base, Expression idx) {
		Expression sz = ((TypeArray)getType(base)).getLength();
		return new ExprBinary(new ExprBinary(idx, ">=", ExprConstInt.zero), "&&",
										 new ExprBinary(idx, "<", sz));
	}

}
