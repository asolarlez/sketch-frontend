package sketch.compiler.passes.preprocessing;

import java.util.List;

import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.exprs.ExprBinary;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprUnary;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssert;

/**
 * Front-end visitor pass for searching the asserts having an LTL formula as
 * condition.
 * 
 * @author Fernando A. Galicia-Mendoza &lt;fmendoza@mit.edu&gt;
 * @version $Id$
 *
 */
public class LTLFiniteA extends FEReplacer {

	private List<Statement> ltls;

	public LTLFiniteA(List<Statement> ltls) {
		this.ltls = ltls;
	}

	public Object visitStmtAssert(StmtAssert stmt) {
		Expression cond = stmt.getCond();

		cond = (Expression) cond.accept(new InnerFin());

		if (cond.getCx() != null && cond.getCx().getLTLAssert()) {
			ltls.add(stmt);
			FEContext ncontext = stmt.getCx();
			ncontext.setLTLAssert(true);
			return new StmtAssert(ncontext, cond, false);
		}

		return super.visitStmtAssert(stmt);
	}

	public List<Statement> getLTLAsserts() {
		return ltls;
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
}
