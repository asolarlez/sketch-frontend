package sketch.compiler.passes.preprocessing;

import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.passes.lowering.SymbolTableVisitor;

public class LTLAnotatePres extends SymbolTableVisitor {

	public LTLAnotatePres() {
		super(null);
	}

	public Object visitStmtAssign(StmtAssign assign) {
		Expression left = assign.getLHS();
		if(left instanceof ExprVar) {
			String name = ((ExprVar) left).getName();
			if (name.startsWith("_pre_")) {
				Type pt = symtab.lookupVar(name.substring(5), assign);
				FEContext nCx = assign.getContext();
				nCx.setLTL(true);
				return new StmtVarDecl(nCx, pt, ((ExprVar) left).getName().substring(1), assign.getRHS());
			}
		}
		return super.visitStmtAssign(assign);
	}
}
