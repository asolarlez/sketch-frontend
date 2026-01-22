package sketch.compiler.main.seq;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.StmtAssert;

public class LTLSeparateAnds extends FEReplacer {

	public Object visitStmtAssert(StmtAssert stmt) {
		if (stmt.getCx().getLTLAssert()) {
			stmt = (StmtAssert) stmt.accept(new ReplaceORs());
			Expression cond = stmt.getCond();
			Separate separating = new Separate();
			cond.accept(separating);
			List<Expression> seps = separating.splits;
			if (seps.size() > 0) {
				Iterator<Expression> it = seps.iterator();
				while (it.hasNext()) {
					this.addStatement(new StmtAssert(stmt.getCx(), it.next(), false));
				}
				return null;
			}
			return stmt;
		}
		return super.visitStmtAssert(stmt);
	}

	class ReplaceORs extends FEReplacer {
		public Object visitExprFunCall(ExprFunCall func) {
			if (func.getName().equals("OR")) {
				List<Expression> params = func.getParams();
				List<Expression> newP = new LinkedList<Expression>();
				Iterator<Expression> it = params.iterator();
				while (it.hasNext()) {
					Expression ei = it.next();
					ei = (Expression) ei.accept(this);
					newP.add(ei);
				}
				return new ExprFunCall(func.getCx(), "||", newP);
			}
			return super.visitExprFunCall(func);
		}
	}

	class Separate extends FEReplacer {
		List<Expression> splits;

		public Separate() {
			splits = new LinkedList<Expression>();
		}

		public Separate(List<Expression> splits) {
			this.splits = splits;
		}

		public Object visitExprFunCall(ExprFunCall func) {
			String fname = func.getName();
			if (fname.equals("AND")) {
				return super.visitExprFunCall(func);
			}
			splits.add(func);
			return func;
		}
	}

	class SearchAnds extends FEReplacer {
		boolean has;

		public SearchAnds() {
			has = false;
		}

		public Object visitExprFunCall(ExprFunCall func) {
			has |= func.getName().equals("AND");
			return super.visitExprFunCall(func);
		}
	}

}
