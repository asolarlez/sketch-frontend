package sketch.compiler.passes.cleanup;

import sketch.compiler.ast.core.exprs.ExprTypeCast;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.passes.lowering.SymbolTableVisitor;



public class MakeCastsExplicit extends SymbolTableVisitor {

	public MakeCastsExplicit() {
		super(null);
		// TODO Auto-generated constructor stub
	}

	
	public Object visitStmtAssign(StmtAssign sa){
		Type tl = getType(sa.getLHS());
		Type tr = getType(sa.getRHS());
		if(tl.isArray()){
			if(!tl.equals(tr)){
				return new StmtAssign(sa.getLHS(), new ExprTypeCast(sa, tl, sa.getRHS())).accept(this);
			}
		}
		return super.visitStmtAssign(sa);
	}
	
}
