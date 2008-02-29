package streamit.frontend.passes;

import streamit.frontend.nodes.ExprArrayRange;
import streamit.frontend.nodes.ExprBinary;
import streamit.frontend.nodes.ExprConstInt;
import streamit.frontend.nodes.ExprTernary;
import streamit.frontend.nodes.ExprVar;
import streamit.frontend.nodes.Expression;
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

	private TempVarGen vargen;
	public ProtectArrayAccesses(TempVarGen vargen){
		super(null);
		this.vargen = vargen;
	}



	@Override
	public Object visitExprArrayRange(ExprArrayRange ear){
		String nname = vargen.nextVar("i");

		assert ear.hasSingleIndex() : "Array ranges not allowed in parallel code.";
		Expression nofset = (Expression) ear.getOffset().accept(this);
		addStatement(new StmtVarDecl(ear, TypePrimitive.inttype, nname,  nofset));
		ExprVar ev = new ExprVar(ear, nname);
		Expression sz = ((TypeArray)getType(ear.getBase())).getLength();
		Expression cond = new ExprBinary(new ExprBinary(ev, ">=", ExprConstInt.zero), "&&",
										 new ExprBinary(ev, "<", sz));
		Expression near = new ExprArrayRange(ear.getBase(), ev);
		Expression tern = new ExprTernary("?:", cond, near, ExprConstInt.zero);
		return tern;
	}

	@Override
    public Object visitStmtAssign(StmtAssign stmt)
    {
		if(stmt.getLHS() instanceof ExprArrayRange){
			ExprArrayRange ear = (ExprArrayRange) stmt.getLHS();
			String nname = vargen.nextVar("i");
			Expression nofset = (Expression) ear.getOffset().accept(this);
			addStatement(new StmtVarDecl(ear, TypePrimitive.inttype, nname,  nofset));
			ExprVar ev = new ExprVar(ear, nname);
			Expression sz = ((TypeArray)getType(ear.getBase())).getLength();
			Expression cond = new ExprBinary(new ExprBinary(ev, ">=", ExprConstInt.zero), "&&",
											 new ExprBinary(ev, "<", sz));
			Expression base = (Expression) ear.getBase().accept(this);
			Expression near = new ExprArrayRange(base, ev);
			return new StmtIfThen(stmt, cond, new StmtAssign(near, (Expression)stmt.getRHS().accept(this)), null);
		}else{
			return super.visitStmtAssign(stmt);
		}
    }

}
