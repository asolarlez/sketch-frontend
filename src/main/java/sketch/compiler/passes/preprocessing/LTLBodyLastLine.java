package sketch.compiler.passes.preprocessing;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtIfThen;

public class LTLBodyLastLine extends FEReplacer {

	private int lastLine;

	public LTLBodyLastLine() {
		lastLine = -1;
	}

	public Object visitStmtAssign(StmtAssign stmt) {

		lastLine = stmt.getCx().getLineNumber();

		return super.visitStmtAssign(stmt);
	}

	public Object visitStmtIfThen(StmtIfThen stmt) {

		lastLine = stmt.getCx().getLineNumber();

		return super.visitStmtIfThen(stmt);
	}

	public int getLastLine() {
		return lastLine;
	}
}
