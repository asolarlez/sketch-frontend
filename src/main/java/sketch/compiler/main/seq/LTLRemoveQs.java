package sketch.compiler.main.seq;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.exprs.ExprArrayInit;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.StmtAssert;
import sketch.util.Pair;
import sketch.util.exceptions.ExceptionAtNode;

public class LTLRemoveQs extends FEReplacer {

	public Object visitStmtAssert(StmtAssert stmt) {
		FEContext context = stmt.getCx();
		if (context.getLTLAssert()) {
			Expression cond = stmt.getCond();
			cond = (Expression) cond.accept(new Sust());
			return new StmtAssert(context, cond, false);
		}
		return super.visitStmtAssert(stmt);
	}

	class Sust extends FEReplacer {

		List<Pair<Expression, Expression>> sust;

		public Sust() {
			sust = new LinkedList<Pair<Expression, Expression>>();
		}

		public Sust(List<Pair<Expression, Expression>> sust) {
			this.sust = sust;
		}

		public Object visitExprVar(ExprVar var) {
			Iterator<Pair<Expression,Expression>> it = sust.iterator();
			while (it.hasNext()) {
				Pair<Expression, Expression> pair = it.next();
				String s1 = var.getName();
				String s2 = ((ExprVar) pair.getFirst()).getName();
				if (s1.equals(s2)) {
					return pair.getSecond();
				}
			}
			return super.visitExprVar(var);
		}

		public Object visitExprFunCall(ExprFunCall func) throws ExceptionAtNode {
			String fname = func.getName();
			if (fname.equals("Forall") || fname.equals("Exists")) {
				Expression linkVar = func.getParams().get(0);
				List<Expression> setFin = ((ExprArrayInit) func.getParams().get(1)).getElements();
				Expression body = func.getParams().get(2);
				if (setFin.size() == 0) {
					return fname.equals("Forall") ? new ExprVar(func.getCx(), "true")
							: new ExprVar(func.getCx(), "false");
				}
				Iterator<Expression> it = setFin.iterator();
				List<Pair<Expression, Expression>> bodies = new LinkedList<Pair<Expression, Expression>>();
				while (it.hasNext()) {
					bodies.add(new Pair<Expression, Expression>(it.next(), body));
				}
				Iterator<Pair<Expression, Expression>> it2 = bodies.iterator();
				Expression newBody = fname.equals("Forall") ? new ExprVar(func.getCx(), "ttrue")
						: new ExprVar(func.getCx(), "ffalse");
				while (it2.hasNext()) {
					Pair<Expression, Expression> pair = it2.next();
					Expression sustSec = pair.getFirst();
					Expression bodyp = pair.getSecond();
					Vars vs = new Vars();
					sustSec.accept(vs);
					List<Expression> vars = vs.getVars();
					boolean inSust = false;
					Iterator<Pair<Expression, Expression>> it1 = sust.iterator();
					while (it1.hasNext()) {
						Pair<Expression, Expression> pair1 = it1.next();
						if (pair1.getFirst().equals(linkVar)) {
							inSust = true;
							break;
						}
					}
					if (inSust || vars.contains(linkVar)) {
						throw new ExceptionAtNode("LTL formulae cannot have shadow variables.", func);
					} else {
						Pair<Expression, Expression> p = new Pair<Expression, Expression>(linkVar, sustSec);
						sust.add(p);
						Sust susti = new Sust(sust);
						bodyp = (Expression) bodyp.accept(susti);
						sust.remove(p);
						List<Expression> newie = new LinkedList<Expression>();
						newie.add(newBody);
						newie.add(bodyp);
						newBody = (Expression) (fname.equals("Forall") ? new ExprFunCall(func.getCx(), "AND", newie)
								: new ExprFunCall(func.getCx(), "OR", newie));
					}
				}
				return newBody;
			}
			return super.visitExprFunCall(func);
		}

		public List<Pair<Expression, Expression>> getSust() {
			return sust;
		}

//		public Object visitExprConstInt(ExprConstInt i) {
//			switch (i.getVal()) {
//			case 0:
//				return new ExprVar(i, "false");
//			case 1:
//				return new ExprVar(i, "true");
//			}
//
//			return super.visitExprConstInt(i);
//		}

	}

	class Vars extends FEReplacer {

		List<Expression> vars;

		public Vars() {
			vars = new LinkedList<Expression>();
		}

		public Object visitExprVar(ExprVar var) {
			vars.add(var);
			return super.visitExprVar(var);
		}

		public List<Expression> getVars() {
			return vars;
		}
	}

}
