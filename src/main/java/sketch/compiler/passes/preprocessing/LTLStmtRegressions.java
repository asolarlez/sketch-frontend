package sketch.compiler.passes.preprocessing;

import java.util.LinkedList;
import java.util.List;

import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.exprs.ExprArrayInit;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprUnary;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssert;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtExpr;
import sketch.compiler.ast.core.stmts.StmtFor;
import sketch.compiler.ast.core.stmts.StmtIfThen;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.stmts.StmtWhile;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.monitor.Graph;

public class LTLStmtRegressions extends FEReplacer {

	private boolean hasLTL;
	private Graph fa;
	private int ltlCurrentLine;

	public LTLStmtRegressions(boolean hasLTL, Graph fa, int ltlCurrentLine) {
		this.hasLTL = hasLTL;
		this.fa = fa;
		this.ltlCurrentLine = ltlCurrentLine;
	}

	public Object visitStmtAssert(StmtAssert stmt) {
		if (stmt.getCx().getLineNumber() == ltlCurrentLine) {
			hasLTL = true;
			initFA(stmt);
			return null;
		}
		return super.visitStmtAssert(stmt);
	}

	public Graph getGraph() {
		return fa;
	}

	public Object visitStmtAssign(StmtAssign stmt) {
		if (hasLTL && stmt.getCx().getLineNumber() > ltlCurrentLine && !stmt.getCx().getLTL()) {
			this.addStatement(stmt);
			createRegression(stmt);
			return null;
		}
		return super.visitStmtAssign(stmt);
	}

	public Object visitStmtVarDecl(StmtVarDecl decl) {
		if (hasLTL && decl.getCx().getLineNumber() > ltlCurrentLine) {
			this.addStatement(decl);
			for (String name : decl.getNames()) {
				createRegression(decl);
			}
			return null;
		}

		return super.visitStmtVarDecl(decl);
	}

	public Object visitStmtExpr(StmtExpr stmt) {
		this.addStatement(stmt);
		createRegression(stmt);
		return null;
	}

	public Object visitStmtIfThen(StmtIfThen cond) {
		if (hasLTL && cond.getCx().getLineNumber() > ltlCurrentLine) {
			Expression stmtCond = cond.getCond();
			Statement condCons = cond.getCons();
			condCons = (Statement) condCons
					.accept(new LTLStmtRegressions(hasLTL, fa, ltlCurrentLine));
			Statement condAlt = cond.getAlt();
			if (condAlt == null) {
				return new StmtIfThen(cond, stmtCond, condCons, null);
			} else {
				condAlt = (Statement) condAlt.accept(new LTLStmtRegressions(hasLTL, fa, ltlCurrentLine));
				return new StmtIfThen(cond, stmtCond, condCons, condAlt);
			}
		}
		return super.visitStmtIfThen(cond);
	}

	public Object visitStmtWhile(StmtWhile loop) {
		if (hasLTL && loop.getCx().getLineNumber() > ltlCurrentLine) {
			Expression loopCond = loop.getCond();
			Statement loopBody = loop.getBody();
			loopBody = (Statement) loopBody.accept(new LTLStmtRegressions(hasLTL, fa, ltlCurrentLine));
			return new StmtWhile(loop, loopCond, loopBody);
		}
		return super.visitStmtWhile(loop);
	}

	public Object visitStmtFor(StmtFor loop) {
		if (hasLTL && loop.getCx().getLineNumber() > ltlCurrentLine) {
			Expression loopCond = loop.getCond();
			Statement loopBody = loop.getBody();
			Statement init = loop.getInit();
			Statement incr = loop.getIncr();
			loopBody = (Statement) loopBody.accept(new LTLStmtRegressions(hasLTL, fa, ltlCurrentLine));
			return new StmtFor(loop, init, loopCond, incr, loopBody, loop.isCanonical());
		}
		return super.visitStmtFor(loop);
	}

	public void initFA(StmtAssert stmt) {

		TypeArray typeAut = new TypeArray(TypePrimitive.bittype, new ExprConstInt(fa.getV()));
		List<Expression> initFA = new LinkedList<Expression>();

		FEContext ncontext = stmt.getCx();
		ncontext.setLTL(true);

		for (int u = 0; u < fa.getV(); u++) {
			if (u == fa.getInitv()) {
				initFA.add(new ExprConstInt(1));
			} else {
				initFA.add(new ExprConstInt(0));
			}
		}
		String autOrName = "st" + fa.getIdA();
		String autCopyName = "stc" + fa.getIdA();

		ExprArrayInit autOrDecl = new ExprArrayInit(ncontext, initFA);
		StmtVarDecl autOr = new StmtVarDecl(ncontext, typeAut, autOrName, autOrDecl);
		StmtVarDecl autCopy = new StmtVarDecl(ncontext, typeAut, autCopyName, autOrDecl);
		StmtVarDecl halt = new StmtVarDecl(ncontext, TypePrimitive.bittype, "h" + fa.getIdA(), new ExprConstInt(0));

		this.addStatement(autOr);
		this.addStatement(autCopy);
		this.addStatement(halt);
	}

	public void createRegression(FENode stmt) {

		for (int u = 0; u < fa.getV(); u++) {
			this.addStatement(fa.makeRegression(stmt, u));
		}

		FEContext curr = stmt.getCx();

		FEContext ncontext = new FEContext(curr.getFileName(), curr.getLineNumber(), curr.getColumnNumber(),
				curr.getComment());
		ncontext.setLTL(true);
		StmtAssign stCopy = new StmtAssign(ncontext, new ExprVar(stmt, "stc" + fa.getIdA()),
				new ExprVar(stmt, "st" + fa.getIdA()));
		this.addStatement(stCopy);

	}

	public boolean getHasLTL() {
		return hasLTL;
	}

	private class ExprRegressions extends FEReplacer {

		public Object visitExprUnary(ExprUnary un) {
			int op = un.getOp();
			if (op >= 4 && op <= 7) {
				createRegression(un);
			}
			return super.visitExprUnary(un);
		}

		public Object visitExprFunCall(ExprFunCall func) {
			createRegression(func);
			return super.visitExprFunCall(func);
		}
	}

}
