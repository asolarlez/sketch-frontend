package sketch.compiler.passes.preprocessing;

import java.util.List;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssert;
import sketch.compiler.ast.core.stmts.StmtFor;

/**
 * Front-end visitor pass deciding if a statement has a now_ prefix.
 * 
 * @author Fernando A. Galicia-Mendoza &lt;fmendoza@mit.edu&gt;
 * @version $Id$
 *
 */
public class LTLNowVars extends FEReplacer {

	private Statement ltlLine;
	private List<String> declare, assign, used;

	public LTLNowVars(Statement ltlLine, List<String> declare, List<String> used) {
		this.ltlLine = ltlLine;
		this.declare = declare;
		this.used = used;
	}

	public Object visitStmtAssert(StmtAssert stmt) {
		if (stmt.equals(ltlLine) && stmt.getCx().getLineNumber() == ltlLine.getCx().getLineNumber()) {
			stmt.accept(new NowVars());
			return stmt;
		}
		return stmt;
	}

	public Object visitStmtFor(StmtFor stmt) {
		if (stmt.equals(ltlLine) && stmt.getCx().getLineNumber() == ltlLine.getCx().getLineNumber()) {
			stmt.accept(new NowVars());
			return stmt;
		}
		return stmt;
	}

	public List<String> getDeclare() {
		return declare;
	}

	public List<String> getUsed() {
		return used;
	}

	class NowVars extends FEReplacer {

		public NowVars() {
		}

		public Object visitExprVar(ExprVar e) {
			String name = e.getName();
			if (name.startsWith("now_")) {
				if (!(used.contains(name))) {
					declare.add(name);
					used.add(name);
				}
			}
			return e;
		}

	}
}