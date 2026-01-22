package sketch.compiler.passes.preprocessing;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.exprs.ExprArrayInit;
import sketch.compiler.ast.core.exprs.ExprArrayRange;
import sketch.compiler.ast.core.exprs.ExprBinary;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprTernary;
import sketch.compiler.ast.core.exprs.ExprUnary;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssert;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.stmts.StmtExpr;
import sketch.compiler.ast.core.stmts.StmtFor;
import sketch.compiler.ast.core.stmts.StmtIfThen;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.stmts.StmtWhile;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.monitor.Graph;

/**
 * Front-end visitor pass for incorporating an RM instrumentation into a sketch.
 * 
 * @author Fernando A. Galicia-Mendoza &lt;fmendoza@mit.edu&gt;
 * @version $Id$
 *
 */
public class LTLRMInstrumentations extends FEReplacer {

	private boolean hasLTL;
	private Graph fa;
	private Statement ltlCurrentLine;
	private List<String> declare;
	private boolean isInf;
	private List<Graph> automataAux;
	private StmtFor LTLLoop;
	private String index;

	public LTLRMInstrumentations(boolean hasLTL, Graph fa, Statement ltlCurrentLine, List<String> declare) {
		this.hasLTL = hasLTL;
		this.fa = fa;
		this.ltlCurrentLine = ltlCurrentLine;
		this.declare = declare;
		this.isInf = false;
		this.automataAux = null;
		this.LTLLoop = null;
	}

	public LTLRMInstrumentations(boolean hasLTL, Graph fa, Statement ltlCurrentLine, List<String> declare,
			boolean isInf) {
		this.hasLTL = hasLTL;
		this.fa = fa;
		this.ltlCurrentLine = ltlCurrentLine;
		this.declare = declare;
		this.isInf = isInf;
		this.automataAux = null;
		this.LTLLoop = null;
	}

	public LTLRMInstrumentations(boolean hasLTL, List<Graph> automataAux, StmtFor LTLLoop, List<String> declare) {
		this.hasLTL = hasLTL;
		this.automataAux = automataAux;
		this.LTLLoop = LTLLoop;
		this.declare = declare;
		this.isInf = false;
	}
	
	public LTLRMInstrumentations(boolean hasLTL, List<Graph> automataAux, StmtFor LTLLoop, List<String> declare,
			String index,
			boolean isInf) {
		this.hasLTL = hasLTL;
		this.automataAux = automataAux;
		this.LTLLoop = LTLLoop;
		this.declare = declare;
		this.isInf = isInf;
		this.index = index;
	}

	public Object visitStmtAssert(StmtAssert stmt) {
		if (stmt.equals(ltlCurrentLine) && stmt.getCx().getLineNumber() == ltlCurrentLine.getCx().getLineNumber()) {
			hasLTL = true;
			if (!isInf) 
				initFA(stmt);
			return null;
		}
		return super.visitStmtAssert(stmt);
	}

	public Graph getGraph() {
		return fa;
	}

	public Object visitStmtAssign(StmtAssign stmt) {
		if (automataAux == null) {
			if (hasLTL && stmt.getCx().getLineNumber() > ltlCurrentLine.getCx().getLineNumber()
					&& !stmt.getCx().getLTL() && !stmt.getCx().getPre()) {
				this.addStatement(stmt);
				if (!isInf)
					createRegression(stmt);
				else
					createRegressionInf(stmt);
				return null;
			}
		} else {
			if (hasLTL && stmt.getCx().getLineNumber() > LTLLoop.getCx().getLineNumber()
					&& !stmt.getCx().getLTL() && !stmt.getCx().getPre()) {
				this.addStatement(stmt);
				if (!isInf)
					createRegression(stmt,automataAux);
				else
					createRegressionInf(stmt, automataAux, index);
				return null;
			}
		}
		return super.visitStmtAssign(stmt);
	}

	public Object visitStmtVarDecl(StmtVarDecl decl) {
		if (decl.getCx().getLTL()) {
			return super.visitStmtVarDecl(decl);
		}
		if (automataAux == null) {
			if (hasLTL && decl.getCx().getLineNumber() > ltlCurrentLine.getCx().getLineNumber()
					&& !decl.getCx().getPreInit()) {
				this.addStatement(decl);
				if (!isInf)
					createRegression(decl);
				else
					createRegressionInf(decl);
				return null;
			}
		} else {
			if (hasLTL && decl.getCx().getLineNumber() > LTLLoop.getCx().getLineNumber()
					&& !decl.getCx().getPreInit()) {
				this.addStatement(decl);
				if (!isInf)
					createRegression(decl, automataAux);
				else
					createRegressionInf(decl, automataAux, index);
				return null;
			}
		}
		return super.visitStmtVarDecl(decl);
	}

	public Object visitStmtExpr(StmtExpr stmt) {
		if (automataAux == null) {
			if (hasLTL && stmt.getCx().getLineNumber() > ltlCurrentLine.getCx().getLineNumber()
					&& !stmt.getCx().getPre()) {
				this.addStatement(stmt);
				if (!isInf)
					createRegression(stmt);
				else
					createRegressionInf(stmt);
				return null;
			}
		} else {
			if (hasLTL && stmt.getCx().getLineNumber() > LTLLoop.getCx().getLineNumber()
					&& !stmt.getCx().getPre()) {
				this.addStatement(stmt);
				if (!isInf)
					createRegression(stmt, automataAux);
				else
					createRegressionInf(stmt, automataAux, index);
				return null;
			}
		}
		return super.visitStmtExpr(stmt);
	}

	public Object visitStmtIfThen(StmtIfThen cond) {
		if (automataAux == null) {
			if (hasLTL && cond.getCx().getLineNumber() > ltlCurrentLine.getCx().getLineNumber()) {
				Expression stmtCond = cond.getCond();
				Statement condCons = cond.getCons();
				condCons = (Statement) condCons
						.accept(new LTLRMInstrumentations(hasLTL, fa, ltlCurrentLine, declare, isInf));
				Statement condAlt = cond.getAlt();
				if (condAlt == null) {
					return new StmtIfThen(cond, stmtCond, condCons, null);
				} else {
					condAlt = (Statement) condAlt
							.accept(new LTLRMInstrumentations(hasLTL, fa, ltlCurrentLine, declare, isInf));
					return new StmtIfThen(cond, stmtCond, condCons, condAlt);
				}
			}
		} else {
			if (hasLTL && cond.getCx().getLineNumber() > LTLLoop.getCx().getLineNumber()) {
				Expression stmtCond = cond.getCond();
				Statement condCons = cond.getCons();
				condCons = (Statement) condCons
						.accept(new LTLRMInstrumentations(hasLTL, automataAux, LTLLoop, declare, index, isInf));
				Statement condAlt = cond.getAlt();
				if (condAlt == null) {
					return new StmtIfThen(cond, stmtCond, condCons, null);
				} else {
					condAlt = (Statement) condAlt
							.accept(new LTLRMInstrumentations(hasLTL, automataAux, LTLLoop, declare, index, isInf));
					return new StmtIfThen(cond, stmtCond, condCons, condAlt);
				}
			}
		}
		return super.visitStmtIfThen(cond);
	}

	public Object visitStmtWhile(StmtWhile loop) {
		if (automataAux == null) {
			if (hasLTL && loop.getCx().getLineNumber() > ltlCurrentLine.getCx().getLineNumber()) {
				Expression loopCond = loop.getCond();
				Statement loopBody = loop.getBody();
				if (!isInf)
					loopBody = (Statement) loopBody
							.accept(new LTLRMInstrumentations(hasLTL, fa, ltlCurrentLine, declare, isInf));
				else
					loopBody = (Statement) loopBody
							.accept(new LTLRMInstrumentations(hasLTL, fa, ltlCurrentLine, declare, isInf));
				return new StmtWhile(loop, loopCond, loopBody);
			}
		} else {
			if (hasLTL && loop.getCx().getLineNumber() > LTLLoop.getCx().getLineNumber()) {
				Expression loopCond = loop.getCond();
				Statement loopBody = loop.getBody();
				if (!isInf)
					loopBody = (Statement) loopBody
							.accept(new LTLRMInstrumentations(hasLTL, automataAux, LTLLoop, declare, index, isInf));
				else
					loopBody = (Statement) loopBody
							.accept(new LTLRMInstrumentations(hasLTL, automataAux, LTLLoop, declare, index, isInf));
				return new StmtWhile(loop, loopCond, loopBody);
			}
		}
		return super.visitStmtWhile(loop);
	}

	public Object visitStmtFor(StmtFor loop) {
		StmtBlock body = (StmtBlock) loop.getBody();
		body = (StmtBlock) body.accept(new OnlyLTLAsserts());
		if (body.size() == 0)
			return loop;
		else {
			if (automataAux == null) {
				if (hasLTL && loop.getCx().getLineNumber() > ltlCurrentLine.getCx().getLineNumber()) {
					Expression loopCond = loop.getCond();
					Statement loopBody = loop.getBody();
					Statement init = loop.getInit();
					Statement incr = loop.getIncr();
					loopBody = (Statement) loopBody
							.accept(new LTLRMInstrumentations(hasLTL, fa, ltlCurrentLine, declare, isInf));
					return new StmtFor(loop, init, loopCond, incr, loopBody, loop.isCanonical());
				}
			} else {
				if (hasLTL && loop.getCx().getLineNumber() > LTLLoop.getCx().getLineNumber()) {
					Expression loopCond = loop.getCond();
					Statement loopBody = loop.getBody();
					Statement init = loop.getInit();
					Statement incr = loop.getIncr();
					loopBody = (Statement) loopBody
							.accept(new LTLRMInstrumentations(hasLTL, automataAux, LTLLoop, declare, index, isInf));
					return new StmtFor(loop, init, loopCond, incr, loopBody, loop.isCanonical());
				}
			}
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

		createRegression(stmt);
	}
	
	public Statement createRegression2(FENode stmt) {

		for (int u = 0; u < fa.getV(); u++) {
			this.addStatement(fa.makeRegression(stmt, u));
		}

		FEContext curr = stmt.getCx();

		FEContext ncontext = new FEContext(curr.getFileName(), curr.getLineNumber(), curr.getColumnNumber(),
				curr.getComment());
		ncontext.setLTL(true);
		StmtAssign stCopy = new StmtAssign(ncontext, new ExprVar(stmt, "stc" + fa.getIdA()),
				new ExprVar(stmt, "st" + fa.getIdA()));
		return stCopy;

	}

	public void createRegression(FENode stmt) {
		FEContext curr = stmt.getCx();
		FEContext ncontext = new FEContext(curr.getFileName(), curr.getLineNumber(), curr.getColumnNumber(),
				curr.getComment());
		ncontext.setLTL(true);
		for (int u = 0; u < fa.getV(); u++) {
			this.addStatement(fa.makeRegression(stmt, u));
		}
		StmtAssign stCopy = new StmtAssign(ncontext, new ExprVar(stmt, "stc" + fa.getIdA()),
				new ExprVar(stmt, "st" + fa.getIdA()));
		this.addStatement(stCopy);
	}

	public void createRegression(FENode stmt, List<Graph> automataAux) {
		FEContext curr = stmt.getCx();
		FEContext ncontext = new FEContext(curr.getFileName(), curr.getLineNumber(), curr.getColumnNumber(),
				curr.getComment());
		ncontext.setLTL(true);
		List<Statement> stmts = new ArrayList<>();
		for (Graph fa : automataAux) {
			for (int u = 0; u < fa.getV(); u++) {
				stmts.add(fa.makeRegression(stmt, u));
			}
			StmtAssign stCopy = new StmtAssign(ncontext, new ExprVar(stmt, "stc" + fa.getIdA()),
					new ExprVar(stmt, "st" + fa.getIdA()));
			stmts.add(stCopy);
		}
		StmtBlock newBody = new StmtBlock(LTLLoop.getContext(), stmts);
		StmtFor newFor = new StmtFor(LTLLoop.getContext(), LTLLoop.getInit(), LTLLoop.getCond(), LTLLoop.getIncr(),
				newBody, LTLLoop.isCanonical());
		this.addStatement(newFor);
	}

	public void createRegressionInf(FENode stmt) {
		FEContext curr = stmt.getCx();
		FEContext ncontext = new FEContext(curr.getFileName(), curr.getLineNumber(), curr.getColumnNumber(),
				curr.getComment());
		ncontext.setLTL(true);
		for (int u = 0; u < fa.getV(); u++) {
			this.addStatement(fa.makeRegression(stmt, u));
		}
		StmtAssign stCopy = new StmtAssign(ncontext, new ExprVar(stmt, "stc" + fa.getIdA()),
				new ExprVar(stmt, "st" + fa.getIdA()));
		this.addStatement(stCopy);
		List<Integer> finalSts = fa.getFinalS();
		Expression cond = new ExprArrayRange(new ExprVar(ncontext, "st" + fa.getIdA()),new ExprConstInt(stmt, finalSts.get(0)));
		for(int i = 1; i < finalSts.size(); i++) {
			Expression right = new ExprArrayRange(new ExprVar(ncontext, "st" + fa.getIdA()),new ExprConstInt(stmt, finalSts.get(i)));
			cond = new ExprBinary(stmt, ExprBinary.BINOP_OR, cond, right);
		}
		Statement acceptFlag = new StmtAssign(ncontext, new ExprVar(ncontext, "accept" + fa.getIdA()),
				new ExprTernary("?:", cond, new ExprConstInt(1), new ExprVar(ncontext, "accept" + fa.getIdA())));
		this.addStatement(acceptFlag);
	}

	public void createRegressionInf(FENode stmt, List<Graph> automataAux, String index) {
		FEContext curr = stmt.getCx();
		FEContext ncontext = new FEContext(curr.getFileName(), curr.getLineNumber(), curr.getColumnNumber(),
				curr.getComment());
		ncontext.setLTL(true);
		List<Statement> stmts = new ArrayList<>();
		for (Graph fa : automataAux) {
			for (int u = 0; u < fa.getV(); u++) {
				stmts.add(fa.makeRegression(stmt, u, index));
			}
			List<Integer> finalSts = fa.getFinalS();
			Expression cond = new ExprArrayRange(
					new ExprArrayRange(new ExprVar(ncontext, "st" + fa.getIdA()), new ExprVar(ncontext, index)),
					new ExprConstInt(ncontext, finalSts.get(0)));
			for (int i = 1; i < finalSts.size(); i++) {
				Expression right = new ExprArrayRange(
						new ExprArrayRange(new ExprVar(ncontext, "st" + fa.getIdA()), new ExprVar(ncontext, index)),
						new ExprConstInt(ncontext, finalSts.get(i)));
				cond = new ExprBinary(stmt, ExprBinary.BINOP_OR, cond, right);
			}
			Statement acceptFlag = new StmtAssign(ncontext,
					new ExprArrayRange(new ExprVar(ncontext, "accept" + fa.getIdA()), new ExprVar(ncontext, index)),
					new ExprTernary("?:", cond, new ExprConstInt(1), new ExprArrayRange(
							new ExprVar(ncontext, "accept" + fa.getIdA()), new ExprVar(ncontext, index))));
			stmts.add(acceptFlag);
		}
		StmtBlock newBody = new StmtBlock(ncontext, stmts);
		StmtFor newFor = new StmtFor(ncontext, LTLLoop.getInit(), LTLLoop.getCond(), LTLLoop.getIncr(),
				newBody, LTLLoop.isCanonical());
		this.addStatement(newFor);
		for (Graph fa : automataAux) {
			StmtAssign stCopy = new StmtAssign(ncontext, new ExprVar(stmt, "stc" + fa.getIdA()),
					new ExprVar(stmt, "st" + fa.getIdA()));
			this.addStatement(stCopy);
		}
	}

	public boolean getHasLTL() {
		return hasLTL;
	}

	class OnlyLTLAsserts extends FEReplacer {

		public Object visitStmtAssert(StmtAssert stmt) {
			Expression cond = stmt.getCond();
			InnerFin inner = new InnerFin();
			cond.accept(inner);
			if (inner.hasLTL)
				return null;
			return super.visitStmtAssert(stmt);
		}
	}

	class InnerFin extends FEReplacer {

		boolean hasLTL;

		InnerFin() {
			hasLTL = false;
		}

		public Object visitExprBinary(ExprBinary bin) {
			Expression left = bin.getLeft();
			Expression right = bin.getRight();

			left = (Expression) left.accept(this);
			right = (Expression) right.accept(this);

			if ((left.getCx() != null || right.getCx() != null)
					&& (left.getCx().getLTLAssert() || right.getCx().getLTLAssert())) {
				FEContext ncontext = bin.getCx();
				ncontext.setLTLAssert(true);
				return new ExprBinary(ncontext, bin);
			}

			return super.visitExprBinary(bin);
		}

		public Object visitExprUnary(ExprUnary un) {
			Expression exp = un.getExpr();

			exp = (Expression) exp.accept(this);

			if (exp.getCx() != null && exp.getCx().getLTLAssert()) {
				FEContext ncontext = exp.getCx();
				ncontext.setLTLAssert(true);
				return new ExprUnary(ncontext, un.getOp(), exp);
			}
			return super.visitExprUnary(un);
		}

		public Object visitExprFunCall(ExprFunCall func) {
			String fName = func.getName();

			if (fName.equals("F") || fName.equals("G") || fName.equals("X") || fName.equals("U")
					|| fName.equals("Forall") || fName.equals("Exists") || fName.equals("||") || fName.equals("&&")
					|| fName.equals("with_help")) {
				FEContext context = func.getCx();
				FEContext ncontext = new FEContext(context.getFileName(), context.getLineNumber(),
						context.getColumnNumber(), context.getComment());
				ncontext.setLTLAssert(true);
				hasLTL = true;
				return new ExprFunCall(ncontext, fName, func.getParams());
			}

			return super.visitExprFunCall(func);
		}

		public boolean getHasLTL() {
			return hasLTL;
		}
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
