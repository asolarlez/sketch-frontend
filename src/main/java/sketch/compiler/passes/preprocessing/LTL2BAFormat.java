/**
 * 
 */
package sketch.compiler.passes.preprocessing;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.exprs.ExprArrayRange;
import sketch.compiler.ast.core.exprs.ExprBinary;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssert;

/**
 * Front-end visitor pass for transforming an sketch predicate into a string
 * that LTL2BA recognizes.
 * 
 * @author Fernando A. Galicia-Mendoza &lt;fmendoza@mit.edu&gt;
 * @version $Id$
 *
 */
public class LTL2BAFormat extends FEReplacer {

	private String ltlString;
	private Statement ltlCurrentLine;
	private Map<Integer, Expression> propNames;
	private int pid;
	private boolean isInf;

	/**
	 * @param symtab
	 */
	public LTL2BAFormat(Statement ltlCurrentLine) {
		this.ltlCurrentLine = ltlCurrentLine;
		this.propNames = new HashMap<Integer, Expression>();
		this.pid = 0;
		this.ltlString = "";
		this.isInf = false;
	}

	public LTL2BAFormat(Statement ltlCurrentLine, boolean isInf) {
		this.ltlCurrentLine = ltlCurrentLine;
		this.propNames = new HashMap<Integer, Expression>();
		this.pid = 0;
		this.ltlString = "";
		this.isInf = isInf;
	}

	public String getLtlString() {
		return ltlString;
	}

	public Map<Integer, Expression> getPropNames() {
		return propNames;
	}

	public Object visitStmtAssert(StmtAssert stmt) {
		Object result = super.visitStmtAssert(stmt);

		Expression cond = stmt.getCond();

		if (stmt.equals(ltlCurrentLine)
				&& (stmt.getCx().getLineNumber() == ltlCurrentLine.getCx().getLineNumber() || isInf)) {
			// Transformation of binary expressions, functions, and variables.
			cond = (Expression) cond.accept(new LTLOperands());
			ltlString = cond.toString();
			return result;
		}
		return result;
	}

	class LTLOperands extends FEReplacer {
		public Object visitExprFunCall(ExprFunCall func) {
			String fname = func.getName();
			if (!(fname.equals("X") || fname.equals("F") || fname.equals("G") || fname.equals("!") || fname.equals("U")
					|| fname.equals("||") || fname.equals("&&"))) {
				return func.accept(new IndexFCVars());
			}
			List<Expression> params = func.getParams();
			List<Expression> newParams = new LinkedList<Expression>();
			Iterator<Expression> it = params.iterator();
			while (it.hasNext()) {
				Expression pi = it.next();
				pi = (Expression) pi.accept(new IndexVars());
				newParams.add(pi);
			}
			return new ExprFunCall(func.getCx(), fname, newParams);
		}
	}

	class IndexVars extends FEReplacer {
		public Object visitExprBinary(ExprBinary bin) {
			if (propNames.containsValue(bin)) {
				for (Map.Entry<Integer, Expression> e : propNames.entrySet()) {
					if (e.getValue().equals(bin)) {
						pid = e.getKey();
					}
				}
			} else {
				pid++;
				propNames.put(pid, bin);
			}
			return new ExprVar(bin.getCx(), "p" + pid);
		}

		public Object visitExprVar(ExprVar var) {
			if (propNames.containsValue(var)) {
				for (Map.Entry<Integer, Expression> e : propNames.entrySet()) {
					if (e.getValue().equals(var)) {
						pid = e.getKey();
					}
				}
			} else {
				pid++;
				propNames.put(pid, var);
			}
			return new ExprVar(var.getCx(), "p" + pid);
		}

		public Object visitExprArrayRange(ExprArrayRange ran) {
			if (propNames.containsValue(ran)) {
				for (Map.Entry<Integer, Expression> e : propNames.entrySet()) {
					if (e.getValue().equals(ran)) {
						pid = e.getKey();
					}
				}
			} else {
				pid++;
				propNames.put(pid, ran);
			}
			return new ExprVar(ran.getCx(), "p" + pid);
		}

		public Object visitExprFunCall(ExprFunCall func) {
			return func.accept(new LTLOperands());
		}
	}

	class IndexFCVars extends FEReplacer {
		public Object visitExprFunCall(ExprFunCall func) {
			if (propNames.containsValue(func)) {
				for (Map.Entry<Integer, Expression> e : propNames.entrySet()) {
					if (e.getValue().equals(func)) {
						pid = e.getKey();
					}
				}
			} else {
				pid++;
				propNames.put(pid, func);
			}
			return new ExprVar(func.getCx(), "p" + pid);
		}
	}
}
