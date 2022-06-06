package sketch.compiler.passes.preprocessing;

import java.util.List;

import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.exprs.ExprBinary;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprUnary;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.StmtAssert;

public class LTLDetective extends FEReplacer {

	private List<Integer> ltlAsserts;

	public LTLDetective(List<Integer> ltlAsserts) {
		this.ltlAsserts = ltlAsserts;
	}

	public Object visitStmtAssert(StmtAssert stmt) {

		FEContext context = stmt.getCx();
		Expression cond = stmt.getCond();

		cond = (Expression) cond.accept(this);

		if (cond.getCx().getLTLAssert()) {
			ltlAsserts.add(stmt.getCx().getLineNumber());
			FEContext ncontext = new FEContext(context.getFileName(), context.getLineNumber(),
					context.getColumnNumber(), context.getComment());
			ncontext.setLTLAssert(true);
			return new StmtAssert(ncontext, cond, false);
		}

		return super.visitStmtAssert(stmt);
	}

	public Object visitExprBinary(ExprBinary bin) {

		FEContext context = bin.getCx();
		Expression left = bin.getLeft();
		Expression right = bin.getRight();

		left = (Expression) left.accept(this);
		right = (Expression) right.accept(this);

		if (left.getCx().getLTLAssert() || right.getCx().getLTLAssert()) {
			FEContext ncontext = new FEContext(context.getFileName(), context.getLineNumber(),
					context.getColumnNumber(), context.getComment());
			ncontext.setLTLAssert(true);
			return new ExprBinary(ncontext, bin);
		}

		return super.visitExprBinary(bin);
	}

	public Object visitExprBinary(ExprUnary un) {
		FEContext context = un.getCx();
		Expression exp = un.getExpr();

		exp = (Expression) exp.accept(this);

		if (exp.getCx().getLTLAssert()) {
			FEContext ncontext = new FEContext(context.getFileName(), context.getLineNumber(),
					context.getColumnNumber(), context.getComment());
			ncontext.setLTLAssert(true);
			return new ExprUnary(ncontext, un.getOp(), exp);
		}
		return super.visitExprUnary(un);
	}

	public Object visitExprFunCall(ExprFunCall func) {
		String fName = func.getName();

		if (fName.equals("F") || fName.equals("G") || fName.equals("X") || fName.equals("U") || fName.equals("R")) {
			FEContext context = func.getCx();
			FEContext ncontext = new FEContext(context.getFileName(), context.getLineNumber(),
					context.getColumnNumber(), context.getComment());
			ncontext.setLTLAssert(true);
			return new ExprFunCall(ncontext, fName, func.getParams());
		}

		return super.visitExprFunCall(func);
	}

	public List<Integer> getLTLAsserts() {
		return ltlAsserts;
	}
}
