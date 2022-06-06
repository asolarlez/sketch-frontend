/**
 * 
 */
package sketch.compiler.passes.preprocessing;

import java.util.HashMap;
import java.util.Map;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.exprs.ExprBinary;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.StmtAssert;

/**
 * @author ferna Two classe:
 */
public class LTL2BAFormat extends FEReplacer {

	private String res;
	private int pid;
	private Map<Integer, Expression> propNames;
	private int ltlCurrentLine;

	/**
	 * @param symtab
	 */
	public LTL2BAFormat(int ltlCurrentLine) {
		res = "";
		pid = 0;
		propNames = new HashMap<Integer, Expression>();
		this.ltlCurrentLine = ltlCurrentLine;
	}

	public Object visitStmtAssert(StmtAssert stmt) {
		Object result = super.visitStmtAssert(stmt);

		Object cond = stmt.getCond();

		if (stmt.getCx().getLTLAssert() && stmt.getCx().getLineNumber() == ltlCurrentLine) {
			visitExprFunCall((ExprFunCall) cond, true);
			return result;
		}

		return result;
	}

	public Object visitExprBinary(ExprBinary bin, boolean print) {
		res += "p" + pid;
		bin = (ExprBinary) bin.accept(new VarCast());
		propNames.put(pid, bin);
		pid++;
		return super.visitExprBinary(bin);
	}

	public Object visitExprVar(ExprVar v, boolean print) {
		res += "p" + pid;
		if (v.getName().equals("true")) {
			propNames.put(pid, ExprConstInt.one);
		} else if (v.getName().equals("false")) {
			propNames.put(pid, ExprConstInt.zero);
		} else {
			propNames.put(pid, v);
		}
		pid++;
		return super.visitExprVar(v);
	}


	public Object visitExprFunCall(ExprFunCall func, boolean print) {
		if (print) {
			String fName = func.getName();
			if (fName.equals("G")) {
				res += "[] (";
				Expression e = func.getParams().get(0);
				if (e instanceof ExprFunCall) {
					visitExprFunCall((ExprFunCall) e, true);
				} else if (e instanceof ExprBinary) {
					visitExprBinary((ExprBinary) e, true);
				} else if (e instanceof ExprVar) {
					visitExprVar((ExprVar) e, true);
				}
				res += ")";
			} else if (fName.equals("F")) {
				res += "<> (";
				Expression e = func.getParams().get(0);
				if (e instanceof ExprFunCall) {
					visitExprFunCall((ExprFunCall) e, true);
				} else if (e instanceof ExprBinary) {
					visitExprBinary((ExprBinary) e, true);
				} else if (e instanceof ExprVar) {
					visitExprVar((ExprVar) e, true);
				}
				res += ")";
			} else if (fName.equals("X")) {
				res += "X (";
				Expression e = func.getParams().get(0);
				if (e instanceof ExprFunCall) {
					visitExprFunCall((ExprFunCall) e, true);
				} else if (e instanceof ExprBinary) {
					visitExprBinary((ExprBinary) e, true);
				} else if (e instanceof ExprVar) {
					visitExprVar((ExprVar) e, true);
				}
				res += ")";
			} else if (fName.equals("!")) {
				res += "! (";
				Expression e = func.getParams().get(0);
				if (e instanceof ExprFunCall) {
					visitExprFunCall((ExprFunCall) e, true);
				} else if (e instanceof ExprBinary) {
					visitExprBinary((ExprBinary) e, true);
				} else if (e instanceof ExprVar) {
					visitExprVar((ExprVar) e, true);
				}
				res += ")";
			} else if (fName.equals("U")) {
				Expression e1 = func.getParams().get(0);
				Expression e2 = func.getParams().get(1);
				res += "(";
				if (e1 instanceof ExprFunCall && e2 instanceof ExprFunCall) {
					visitExprFunCall((ExprFunCall) e1, true);
					res += " U ";
					visitExprFunCall((ExprFunCall) e2, true);
					res += ")";
				} else if (e1 instanceof ExprBinary && e2 instanceof ExprFunCall) {
					visitExprBinary((ExprBinary) e1, true);
					res += " U ";
					visitExprFunCall((ExprFunCall) e2, true);
					res += ")";
				} else if (e1 instanceof ExprFunCall && e2 instanceof ExprBinary) {
					visitExprFunCall((ExprFunCall) e1, true);
					res += " U ";
					visitExprBinary((ExprBinary) e2, true);
					res += ")";
				} else if (e1 instanceof ExprBinary && e2 instanceof ExprBinary) {
					visitExprBinary((ExprBinary) e1, true);
					res += " U ";
					visitExprBinary((ExprBinary) e2, true);
					res += ")";
				} else if (e1 instanceof ExprVar && e2 instanceof ExprFunCall) {
					visitExprVar((ExprVar) e1, true);
					res += " U ";
					visitExprFunCall((ExprFunCall) e2, true);
					res += ")";
				} else if (e1 instanceof ExprFunCall && e2 instanceof ExprVar) {
					visitExprFunCall((ExprFunCall) e1, true);
					res += " U ";
					visitExprVar((ExprVar) e2, true);
					res += ")";
				} else if (e1 instanceof ExprVar && e2 instanceof ExprVar) {
					visitExprVar((ExprVar) e1, true);
					res += " U ";
					visitExprVar((ExprVar) e2, true);
					res += ")";
				} else if (e1 instanceof ExprVar && e2 instanceof ExprBinary) {
					visitExprVar((ExprVar) e1, true);
					res += " U ";
					visitExprBinary((ExprBinary) e2, true);
					res += ")";
				} else if (e1 instanceof ExprBinary && e2 instanceof ExprVar) {
					visitExprBinary((ExprBinary) e1, true);
					res += " U ";
					visitExprVar((ExprVar) e2, true);
					res += ")";
				}
			} else if (fName.equals("V")) {
				Expression e1 = func.getParams().get(0);
				Expression e2 = func.getParams().get(1);
				res += "(";
				if (e1 instanceof ExprFunCall && e2 instanceof ExprFunCall) {
					visitExprFunCall((ExprFunCall) e1, true);
					res += " V ";
					visitExprFunCall((ExprFunCall) e2, true);
					res += ")";
				} else if (e1 instanceof ExprBinary && e2 instanceof ExprFunCall) {
					visitExprBinary((ExprBinary) e1, true);
					res += " V ";
					visitExprFunCall((ExprFunCall) e2, true);
					res += ")";
				} else if (e1 instanceof ExprFunCall && e2 instanceof ExprBinary) {
					visitExprFunCall((ExprFunCall) e1, true);
					res += " V ";
					visitExprBinary((ExprBinary) e2, true);
					res += ")";
				} else if (e1 instanceof ExprBinary && e2 instanceof ExprBinary) {
					visitExprBinary((ExprBinary) e1, true);
					res += " V ";
					visitExprBinary((ExprBinary) e2, true);
					res += ")";
				} else if (e1 instanceof ExprVar && e2 instanceof ExprFunCall) {
					visitExprVar((ExprVar) e1, true);
					res += " V ";
					visitExprFunCall((ExprFunCall) e2, true);
					res += ")";
				} else if (e1 instanceof ExprFunCall && e2 instanceof ExprVar) {
					visitExprFunCall((ExprFunCall) e1, true);
					res += " V ";
					visitExprVar((ExprVar) e2, true);
					res += ")";
				} else if (e1 instanceof ExprVar && e2 instanceof ExprVar) {
					visitExprVar((ExprVar) e1, true);
					res += " V ";
					visitExprVar((ExprVar) e2, true);
					res += ")";
				} else if (e1 instanceof ExprVar && e2 instanceof ExprBinary) {
					visitExprVar((ExprVar) e1, true);
					res += " V ";
					visitExprBinary((ExprBinary) e2, true);
					res += ")";
				} else if (e1 instanceof ExprBinary && e2 instanceof ExprVar) {
					visitExprBinary((ExprBinary) e1, true);
					res += " V ";
					visitExprVar((ExprVar) e2, true);
					res += ")";
				}
			} else if (fName.equals("&&")) {
				Expression e1 = func.getParams().get(0);
				Expression e2 = func.getParams().get(1);
				res += "(";
				if (e1 instanceof ExprFunCall && e2 instanceof ExprFunCall) {
					visitExprFunCall((ExprFunCall) e1, true);
					res += " && ";
					visitExprFunCall((ExprFunCall) e2, true);
					res += ")";
				} else if (e1 instanceof ExprBinary && e2 instanceof ExprFunCall) {
					visitExprBinary((ExprBinary) e1, true);
					res += " && ";
					visitExprFunCall((ExprFunCall) e2, true);
					res += ")";
				} else if (e1 instanceof ExprFunCall && e2 instanceof ExprBinary) {
					visitExprFunCall((ExprFunCall) e1, true);
					res += " && ";
					visitExprBinary((ExprBinary) e2, true);
					res += ")";
				} else if (e1 instanceof ExprBinary && e2 instanceof ExprBinary) {
					visitExprBinary((ExprBinary) e1, true);
					res += " && ";
					visitExprBinary((ExprBinary) e2, true);
					res += ")";
				} else if (e1 instanceof ExprVar && e2 instanceof ExprFunCall) {
					visitExprVar((ExprVar) e1, true);
					res += " && ";
					visitExprFunCall((ExprFunCall) e2, true);
					res += ")";
				} else if (e1 instanceof ExprFunCall && e2 instanceof ExprVar) {
					visitExprFunCall((ExprFunCall) e1, true);
					res += " && ";
					visitExprVar((ExprVar) e2, true);
					res += ")";
				} else if (e1 instanceof ExprVar && e2 instanceof ExprVar) {
					visitExprVar((ExprVar) e1, true);
					res += " && ";
					visitExprVar((ExprVar) e2, true);
					res += ")";
				} else if (e1 instanceof ExprVar && e2 instanceof ExprBinary) {
					visitExprVar((ExprVar) e1, true);
					res += " && ";
					visitExprBinary((ExprBinary) e2, true);
					res += ")";
				} else if (e1 instanceof ExprBinary && e2 instanceof ExprVar) {
					visitExprBinary((ExprBinary) e1, true);
					res += " && ";
					visitExprVar((ExprVar) e2, true);
					res += ")";
				}
			} else if (fName.equals("||")) {
				Expression e1 = func.getParams().get(0);
				Expression e2 = func.getParams().get(1);
				res += "(";
				if (e1 instanceof ExprFunCall && e2 instanceof ExprFunCall) {
					visitExprFunCall((ExprFunCall) e1, true);
					res += " || ";
					visitExprFunCall((ExprFunCall) e2, true);
					res += ")";
				} else if (e1 instanceof ExprBinary && e2 instanceof ExprFunCall) {
					visitExprBinary((ExprBinary) e1, true);
					res += " || ";
					visitExprFunCall((ExprFunCall) e2, true);
					res += ")";
				} else if (e1 instanceof ExprFunCall && e2 instanceof ExprBinary) {
					visitExprFunCall((ExprFunCall) e1, true);
					res += " || ";
					visitExprBinary((ExprBinary) e2, true);
					res += ")";
				} else if (e1 instanceof ExprBinary && e2 instanceof ExprBinary) {
					visitExprBinary((ExprBinary) e1, true);
					res += " || ";
					visitExprBinary((ExprBinary) e2, true);
					res += ")";
				} else if (e1 instanceof ExprVar && e2 instanceof ExprFunCall) {
					visitExprVar((ExprVar) e1, true);
					res += " || ";
					visitExprFunCall((ExprFunCall) e2, true);
					res += ")";
				} else if (e1 instanceof ExprFunCall && e2 instanceof ExprVar) {
					visitExprFunCall((ExprFunCall) e1, true);
					res += " || ";
					visitExprVar((ExprVar) e2, true);
					res += ")";
				} else if (e1 instanceof ExprVar && e2 instanceof ExprVar) {
					visitExprVar((ExprVar) e1, true);
					res += " || ";
					visitExprVar((ExprVar) e2, true);
					res += ")";
				} else if (e1 instanceof ExprVar && e2 instanceof ExprBinary) {
					visitExprVar((ExprVar) e1, true);
					res += " || ";
					visitExprBinary((ExprBinary) e2, true);
					res += ")";
				} else if (e1 instanceof ExprBinary && e2 instanceof ExprVar) {
					visitExprBinary((ExprBinary) e1, true);
					res += " || ";
					visitExprVar((ExprVar) e2, true);
					res += ")";
				}
			} else {
				/*
				 * res += fName + "("; for (int i = 0; i <
				 * func.getParams().size(); ++i) { res += (i != 0) ? "," : "" +
				 * func.getParams().get(i); } res += ")";
				 */
				res += "p" + pid;
				propNames.put(pid, func);
				pid++;
			}
		}

		return super.visitExprFunCall(func);
	}

	public String ltlString() {
		return res;
	}

	public Map<Integer, Expression> getPropTable() {
		return propNames;
	}

	class VarCast extends FEReplacer {

		public VarCast() {
		}

		public Object visitExprVar(ExprVar v) {
			
			String vN = v.getName();

			if (vN.equals("true")) {
				return new ExprConstInt(v, 1);
			} else if (vN.equals("false")) {
				return new ExprConstInt(v, 0);
			}

			return super.visitExprVar(v);
		}

	}
}
