package sketch.compiler.passes.preprocessing;

import java.util.Iterator;
import java.util.List;

import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.exprs.ExprArrayRange;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssert;
import sketch.compiler.ast.core.stmts.StmtFor;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.TypePrimitive;

public class LTLNewInits extends FEReplacer {

	private Statement ltlLine;
	private List<String> declare, modVars;
	private int idST;

	public LTLNewInits(Statement ltlLine, List<String> declare, int idST) {
		this.ltlLine = ltlLine;
		this.declare = declare;
		this.idST = idST;
	}

	public Object visitStmtAssert(StmtAssert stmt) {
		if (stmt.equals(stmt) && stmt.getCx().getLineNumber() == ltlLine.getCx().getLineNumber()) {
			Iterator<String> it = declare.iterator();
			while (it.hasNext()) {
				String name = it.next();
				FEContext curr = stmt.getCx();
				FEContext ncontext = new FEContext(curr.getFileName(), curr.getLineNumber(), curr.getColumnNumber(),
						curr.getComment());
				ncontext.setPreInit(true);
				StmtVarDecl prim = new StmtVarDecl(ncontext, TypePrimitive.inttype, name + idST,
						new ExprVar(ncontext, name.substring(4)));
				this.addStatement(prim);
				stmt = (StmtAssert) stmt.accept(new ReplaceNowVars(name, idST));
			}
		}
		return super.visitStmtAssert(stmt);
	}

	public Object visitStmtFor(StmtFor stmt) {
		if (stmt.equals(stmt) && stmt.getCx().getLineNumber() == ltlLine.getCx().getLineNumber()) {
			Iterator<String> it = declare.iterator();
			while (it.hasNext()) {
				String name = it.next();
				FEContext curr = stmt.getCx();
				FEContext ncontext = new FEContext(curr.getFileName(), curr.getLineNumber(), curr.getColumnNumber(),
						curr.getComment());
				ncontext.setPreInit(true);
				StmtVarDecl prim = new StmtVarDecl(ncontext, TypePrimitive.inttype, name + idST,
						new ExprVar(ncontext, name.substring(4)));
				this.addStatement(prim);
				stmt = (StmtFor) stmt.accept(new ReplaceNowVars(name, idST));
			}
		}
		return super.visitStmtFor(stmt);
	}

	class ModifiedVariable extends FEReplacer {

		private boolean isArr;

		public ModifiedVariable() {
			this.isArr = false;
		}

		public Object visitExprVar(ExprVar var) {
			if (!this.isArr)
				modVars.add(var.getName());
			return super.visitExprVar(var);
		}

		public Object visitExprArrayRange(ExprArrayRange stmt) {
			this.isArr = true;
			return super.visitExprArrayRange(stmt);
		}
	}

	class ReplaceNowVars extends FEReplacer {

		String nowName;
		int ltlLine;

		public ReplaceNowVars(String nowName, int ltlLine) {
			this.nowName = nowName;
			this.ltlLine = ltlLine;
		}

		public Object visitExprVar(ExprVar var) {
			if (var.getName().equals(nowName)) {
				return new ExprVar(var.getCx(), nowName + ltlLine);
			}
			return super.visitExprVar(var);
		}
	}

}
