package sketch.compiler.passes.preprocessing;

import java.util.List;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssert;
import sketch.compiler.ast.core.stmts.StmtFor;

/**
 * Front-end visitor pass deciding if a statement has a pre_ prefix.
 * 
 * @author Fernando A. Galicia-Mendoza &lt;fmendoza@mit.edu&gt;
 * @version $Id$
 *
 */
public class LTLPreVars extends FEReplacer {

	private Statement ltlLine;
	private List<String> declare, assign, used;
	private boolean isInf;

	public LTLPreVars(Statement ltlLine, List<String> declare, List<String> assign, List<String> used) {
		this.ltlLine = ltlLine;
		this.declare = declare;
		this.assign = assign;
		this.used = used;
		this.isInf = false;
	}

	public LTLPreVars(Statement ltlLine, List<String> declare, List<String> assign, List<String> used, boolean isInf) {
		this.ltlLine = ltlLine;
		this.declare = declare;
		this.assign = assign;
		this.used = used;
		this.isInf = isInf;
	}

	public Object visitStmtAssert(StmtAssert stmt) {
		if (stmt.equals(ltlLine) && (stmt.getCx().getLineNumber() == ltlLine.getCx().getLineNumber() || isInf)) {
			stmt.accept(new PreVars());
			return stmt;
		}
		return stmt;
	}

	public Object visitStmtFor(StmtFor stmt) {
		if (stmt.equals(ltlLine) && (stmt.getCx().getLineNumber() == ltlLine.getCx().getLineNumber() || isInf)) {
			stmt.accept(new PreVars());
			return stmt;
		}
		return stmt;
	}

	public List<String> getDeclare() {
		return declare;
	}

	public List<String> getAssign() {
		return assign;
	}

	public List<String> getUsed() {
		return used;
	}

	class PreVars extends FEReplacer {
		
		public PreVars() {
		}

		public Object visitExprVar(ExprVar e) {
			String name = e.getName();
			if (name.startsWith("pre_")) {
				if (!(used.contains(name))) {
					declare.add(name);
					assign.add(name);
					used.add(name);
				}
			}
			return e;
		}

	}
}