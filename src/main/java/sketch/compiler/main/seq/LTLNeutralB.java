package sketch.compiler.main.seq;

import java.util.LinkedList;
import java.util.List;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;

public class LTLNeutralB extends FEReplacer {

	public Object visitExprFunCall(ExprFunCall func) {
		SearchBV search = new SearchBV();
		func.accept(search);
		while (search.has) {
			String fname = func.getName();
			List<Expression> params = func.getParams();
			if (fname.equals("AND") && params.size() == 2) {
				Expression left = params.get(0);
				Expression right = params.get(1);
				if (left instanceof ExprVar) {
					String name = ((ExprVar) left).getName();
					if (name.equals("ffalse")) {
						return new ExprVar(func.getCx(), "ffalse");
					}
					if (name.equals("ttrue")) {
						return right.accept(this);
					}
				}
				if (right instanceof ExprVar) {
					String name = ((ExprVar) right).getName();
					if (name.equals("ffalse")) {
						return new ExprVar(func.getCx(), "ffalse");
					}
					if (name.equals("ttrue")) {
						return left.accept(this);
					}
				}
				left = (Expression) left.accept(this);
				right = (Expression) right.accept(this);
				List<Expression> newies = new LinkedList<Expression>();
				newies.add(left);
				newies.add(right);
				return (new ExprFunCall(func.getCx(), fname, newies)).accept(this);
			}
			if (fname.equals("OR") && params.size() == 2) {
				Expression left = params.get(0);
				Expression right = params.get(1);
				if (left instanceof ExprVar) {
					String name = ((ExprVar) left).getName();
					if (name.equals("ttrue")) {
						return new ExprVar(func.getCx(), "ttrue");
					}
					if (name.equals("ffalse")) {
						return right.accept(this);
					}
				}
				if (right instanceof ExprVar) {
					String name = ((ExprVar) right).getName();
					if (name.equals("ttrue")) {
						return new ExprVar(func.getCx(), "ttrue");
					}
					if (name.equals("ffalse")) {
						return left.accept(this);
					}
				}
				left = (Expression) left.accept(this);
				right = (Expression) right.accept(this);
				List<Expression> newies = new LinkedList<Expression>();
				newies.add(left);
				newies.add(right);
				return (new ExprFunCall(func.getCx(), fname, newies)).accept(this);
			}
		}
		return super.visitExprFunCall(func);
	}

	class SearchBV extends FEReplacer {

		boolean has;

		public SearchBV() {
			has = false;
		}

		public Object visitExprVar(ExprVar var) {
			has |= var.getName().equals("ttrue") || var.getName().equals("ffalse");
			return super.visitExprVar(var);
		}

	}

}
