package sketch.compiler.passes.preprocessing;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.stmts.StmtAssert;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtVarDecl;

public class LTLRemoving extends FEReplacer {

	public LTLRemoving() {
	}

	public Object visitStmtAssign(StmtAssign stmt) {
		Object result = super.visitStmtAssign(stmt);

		if (stmt.getCx().getLTL()) {
			return null;
		}

		return result;
	}

	public Object visitStmtVarDecl(StmtVarDecl stmt) {
		Object result = super.visitStmtVarDecl(stmt);

		if (stmt.getCx().getLTL()) {
			return null;
		}

		return result;
	}

	public Object visitStmtAssert(StmtAssert stmt) {
		if (stmt.getCx().getLTL()) {
			return null;
		}
		return super.visitStmtAssert(stmt);
	}

}
