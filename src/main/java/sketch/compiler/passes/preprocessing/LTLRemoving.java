package sketch.compiler.passes.preprocessing;

import java.util.List;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssert;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtAssume;
import sketch.compiler.ast.core.stmts.StmtFor;
import sketch.compiler.ast.core.stmts.StmtIfThen;
import sketch.compiler.ast.core.stmts.StmtVarDecl;

public class LTLRemoving extends FEReplacer {

	List<Statement> ltlAsserts;

	public LTLRemoving(List<Statement> ltlAsserts) {
		this.ltlAsserts = ltlAsserts;
	}

	public Object visitStmtAssign(StmtAssign stmt) {
		Object result = super.visitStmtAssign(stmt);

		if (stmt.getCx().getLTL() || stmt.getCx().getPre() || stmt.getCx().getPreInit()) {
			return null;
		}

		return result;
	}

	public Object visitStmtVarDecl(StmtVarDecl stmt) {
		Object result = super.visitStmtVarDecl(stmt);

		if (stmt.getCx().getLTL() || stmt.getCx().getPre() || stmt.getCx().getPreInit()) {
			return null;
		}
		return result;
	}

	public Object visitStmtAssert(StmtAssert stmt) {
		if (stmt.getCx().getLTL() || stmt.getCx().getPre() || stmt.getCx().getPreInit()) {
			return null;
		}
		if (ltlAsserts.contains(stmt)) {
			return null;
		}
		return super.visitStmtAssert(stmt);
	}

	public Object visitStmtFor(StmtFor stmt) {
		if (stmt.getCx().getLTL() || stmt.getCx().getPre() || stmt.getCx().getPreInit()) {
			return null;
		}
		if (ltlAsserts.contains(stmt)) {
			return null;
		}
		return super.visitStmtFor(stmt);
	}

	public Object visitStmtAssume(StmtAssume stmt) {
		if (stmt.getCx().getLTL() || stmt.getCx().getPre() || stmt.getCx().getPreInit()) {
			return null;
		}
		return super.visitStmtAssume(stmt);
	}

	public Object visitStmtIfThen(StmtIfThen stmt) {
		if (stmt.getCx().getLTL() || stmt.getCx().getPre() || stmt.getCx().getPreInit()) {
			return null;
		}
		return super.visitStmtIfThen(stmt);
	}

	public Object visitExprFunCall(ExprFunCall stmt) {
		if (stmt.getCx().getLTL() || stmt.getCx().getPre() || stmt.getCx().getPreInit()) {
			return null;
		}
		return super.visitExprFunCall(stmt);
	}

	public Object visitFunction(Function func) {
		if (func.getName().startsWith("access_")) {
			return null;
		}
		return super.visitFunction(func);
	}

}
