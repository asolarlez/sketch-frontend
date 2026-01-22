package sketch.compiler.main.seq;

import java.util.LinkedList;
import java.util.List;

import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssert;

public class LTLQCNF extends FEReplacer {

	public Object visitStmtAssert(StmtAssert stmt) {
		FEContext context = stmt.getCx();

		if (context.getLTLAssert()) {
			Expression cond = stmt.getCond();
			QCNF cnf = new QCNF();
			cond = (Expression) cond.accept(cnf);
			return new StmtAssert(stmt.getCx(), cond, false);
		}

		return super.visitStmtAssert(stmt);
	}

	class QCNF extends FEReplacer {

		List<Statement> newConjuncts = new LinkedList<Statement>();

		public Object visitExprFunCall(ExprFunCall func) {
			String fname = func.getName();
			if (!(fname.equals("AND") || fname.equals("OR"))) {
				return super.visitExprFunCall(func);
			}
			if (fname.equals("AND")) {
				List<Expression> conjuncts = func.getParams();
				Expression left = (Expression) conjuncts.get(0).accept(this);
				Expression right = (Expression) conjuncts.get(1).accept(this);
				List<Expression> conjs = new LinkedList<Expression>();
				conjs.add(left);
				conjs.add(right);
				return new ExprFunCall(func.getCx(), "AND", conjs);
			}
			if (fname.equals("OR")) {
				List<Expression> literals = func.getParams();
				Expression left = (Expression) literals.get(0).accept(this);
				Expression right = (Expression) literals.get(1).accept(this);
				return distr(func.getCx(), left, right);
			}
			return super.visitExprFunCall(func);
		}

		private Expression distr(FEContext context, Expression left, Expression right) {
			if (left instanceof ExprFunCall) {
				String fname = ((ExprFunCall) left).getName();
				if (fname.equals("AND")) {
					Expression p11 = ((ExprFunCall) left).getParams().get(0);
					Expression p12 = ((ExprFunCall) left).getParams().get(1);
					Expression newLeft = distr(context, p11, right);
					Expression newRight = distr(context, p12, right);
					List<Expression> newies = new LinkedList<Expression>();
					newies.add(newRight);
					newies.add(newLeft);
					return new ExprFunCall(context, "AND", newies);
				}
			}
			if (right instanceof ExprFunCall) {
				String fname = ((ExprFunCall) right).getName();
				if (fname.equals("AND")) {
					Expression p11 = ((ExprFunCall) right).getParams().get(0);
					Expression p12 = ((ExprFunCall) right).getParams().get(1);
					Expression newLeft = distr(context, left, p11);
					Expression newRight = distr(context, left, p12);
					List<Expression> newies = new LinkedList<Expression>();
					newies.add(newRight);
					newies.add(newLeft);
					return new ExprFunCall(context, "AND", newies);
				}
			}
			List<Expression> newies = new LinkedList<Expression>();
			newies.add(left);
			newies.add(right);
			return new ExprFunCall(context, "OR", newies);
		}

	}

}
