package sketch.compiler.passes.preprocessing;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.SymbolTable;
import sketch.compiler.ast.core.exprs.ExprArrayRange;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssert;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.stmts.StmtFor;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.passes.lowering.SymbolTableVisitor;

public class LTLPreAssigns extends SymbolTableVisitor {

	private Statement ltlLine;
	private List<String> declare, assign, used, modVars;
	private boolean isInf;

	public LTLPreAssigns(Statement ltlLine, List<String> declare, List<String> assign, List<String> used,
			SymbolTable symtab) {
		super(symtab);
		this.ltlLine = ltlLine;
		this.declare = declare;
		this.assign = assign;
		this.used = used;
		this.isInf = false;
	}

	public LTLPreAssigns(Statement ltlLine, List<String> declare, List<String> assign, List<String> used,
			boolean isInf, SymbolTable symtab) {
		super(symtab);
		this.ltlLine = ltlLine;
		this.declare = declare;
		this.assign = assign;
		this.isInf = isInf;
		this.used = used;
	}

	public Object visitStmtBlock(StmtBlock block) {
		if (isInf) {
			List<Statement> stmts = new ArrayList<>();
			for (Statement st : block.getStmts()) {
				stmts.add(st);
			}
			Iterator<String> it = declare.iterator();
			while (it.hasNext()) {
				String name = it.next();
				FEContext curr = block.getCx();
				FEContext ncontext = new FEContext(curr.getFileName(), curr.getLineNumber(), curr.getColumnNumber(),
						curr.getComment());
				ncontext.setPreInit(true);
				if (!isInf) {
					StmtVarDecl prim = new StmtVarDecl(ncontext, symtab.lookupVar(name.substring(4), block), name,
							new ExprVar(ncontext, name.substring(4)));
					stmts.add(0, prim);
				} else {
					ncontext.setLTL(true);
					StmtAssign preN = new StmtAssign(ncontext, new ExprVar(ncontext, "_" + name),
							new ExprVar(ncontext, name.substring(4)));
					stmts.add(0, preN);
				}

			}
			return new StmtBlock(block, stmts);
		}
		return super.visitStmtBlock(block);
	}

	public Object visitStmtAssert(StmtAssert stmt) {
		if (stmt.equals(ltlLine) && stmt.getCx().getLineNumber() == ltlLine.getCx().getLineNumber()) {
			Iterator<String> it = declare.iterator();
			while (it.hasNext()) {
				String name = it.next();
				FEContext curr = stmt.getCx();
				FEContext ncontext = new FEContext(curr.getFileName(), curr.getLineNumber(), curr.getColumnNumber(),
						curr.getComment());
				ncontext.setPreInit(true);
				StmtVarDecl prim = new StmtVarDecl(ncontext, symtab.lookupVar(name.substring(4), stmt), name,
						new ExprVar(ncontext, name.substring(4)));
				this.addStatement(prim);
			}
		}
		return super.visitStmtAssert(stmt);
	}

	public Object visitStmtFor(StmtFor stmt) {
		if (stmt.equals(ltlLine) && stmt.getCx().getLineNumber() == ltlLine.getCx().getLineNumber()) {
			Iterator<String> it = declare.iterator();
			while (it.hasNext()) {
				String name = it.next();
				FEContext curr = stmt.getCx();
				FEContext ncontext = new FEContext(curr.getFileName(), curr.getLineNumber(), curr.getColumnNumber(),
						curr.getComment());
				ncontext.setPreInit(true);
				StmtVarDecl prim = new StmtVarDecl(ncontext, symtab.lookupVar(name.substring(4), stmt), name,
						new ExprVar(ncontext, name.substring(4)));
				this.addStatement(prim);
			}
		}
		return super.visitStmtFor(stmt);
	}

	public Object visitStmtAssign(StmtAssign stmt) {
		if (stmt.getCx().getLineNumber() > ltlLine.getCx().getLineNumber() && !stmt.getCx().getPreInit()) {
			modVars = new LinkedList<String>();
			ModifiedVariable mv = new ModifiedVariable(symtab);
			stmt.getLHS().accept(mv);
			Iterator<String> it = assign.iterator();
			while (it.hasNext()) {
				String name = it.next();
				if (modVars.contains(name.substring(4))) {
					FEContext curr = stmt.getCx();
					FEContext ncontext = new FEContext(curr.getFileName(), curr.getLineNumber(), curr.getColumnNumber(),
							curr.getComment());
					ncontext.setPre(true);
					StmtAssign preN = new StmtAssign(ncontext, new ExprVar(ncontext, name),
							new ExprVar(ncontext, name.substring(4)));
					this.addStatement(preN);
				}
			}
			return super.visitStmtAssign(stmt);
		}
		return super.visitStmtAssign(stmt);
	}

	class ModifiedVariable extends SymbolTableVisitor {

		private boolean isArr;

		public ModifiedVariable(SymbolTable symtab) {
			super(symtab);
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

}
