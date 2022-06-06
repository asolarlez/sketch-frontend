package sketch.compiler.passes.preprocessing;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.stmts.StmtAssert;
import sketch.compiler.ast.core.stmts.StmtAssume;

public class LTLRemoveAsserts extends FEReplacer {

	public Object visitStmtAssert(StmtAssert stmt) {
		return null;
	}

	public Object visitStmtAssume(StmtAssume stmt) {
		return null;
	}

}
