package sketch.compiler.passes.preprocessing;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.exprs.ExprBinary;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprUnary;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.util.exceptions.ExceptionAtNode;

/**
 * Front-end visitor pass for verifying that LTL expressions are syntactically
 * well-formed.
 * 
 * @author Fernando Abigail Galicia-Mendoza &lt;fmendoza@mit.edu&gt;
 * @version $Id$
 *
 */
public class LTLWFExpression extends FEReplacer {

	private boolean ltlWarning;

	public LTLWFExpression() {
		ltlWarning = false;
	}

	@Override
	public Object visitExprBinary(ExprBinary bin) throws ExceptionAtNode {
		Expression left = bin.getLeft();
		Expression right = bin.getRight();

		int currentOp = bin.getOp();

		if (currentOp != ExprBinary.BINOP_AND && currentOp != ExprBinary.BINOP_OR) {
			if (left instanceof ExprFunCall) {
				ExprFunCall funcL = (ExprFunCall) left;
				if (funcL.getName().equals("F") || funcL.getName().equals("G") || funcL.getName().equals("U")
						|| funcL.getName().equals("X") || funcL.getName().equals("R")) {
					throw new ExceptionAtNode("ill-formed LTL formula. 1", bin);
				}
			}
			if (right instanceof ExprFunCall) {
				ExprFunCall funcL = (ExprFunCall) right;
				if (funcL.getName().equals("F") || funcL.getName().equals("G") || funcL.getName().equals("U")
						|| funcL.getName().equals("X") || funcL.getName().equals("R")) {
					throw new ExceptionAtNode("ill-formed LTL formula. 2", bin);
				}
			}
		}

		if (left instanceof ExprFunCall) {
			ExprFunCall funcL = (ExprFunCall) left;
			this.visitExprFunCall(funcL);
		}

		if (right instanceof ExprFunCall) {
			ExprFunCall funcR = (ExprFunCall) right;
			this.visitExprFunCall(funcR);
		}

		return super.visitExprBinary(bin);
	}

	@Override
	public Object visitExprUnary(ExprUnary un) throws ExceptionAtNode {
		Expression exp = un.getExpr();

		if (un.getOp() != ExprUnary.UNOP_NOT) {
			if (exp instanceof ExprFunCall) {
				ExprFunCall func = (ExprFunCall) exp;
				if (func.getName().equals("F") || func.getName().equals("G") || func.getName().equals("U")
						|| func.getName().equals("X") || func.getName().equals("R")) {
					throw new ExceptionAtNode("ill-formed LTL formula. 3", un);
				}
			}
		}

		return super.visitExprUnary(un);
	}

	@Override
	public Object visitExprFunCall(ExprFunCall func) throws ExceptionAtNode {
		String fName = func.getName();

		if (!(fName.equals("F") || fName.equals("G") || fName.equals("X") || fName.equals("R"))) {
			ltlWarning = true;
			for (Expression e : func.getParams()) {
				if (e instanceof ExprFunCall) {
					ExprFunCall inner = (ExprFunCall) e;
					this.visitExprFunCall(inner);
				}
				if (e instanceof ExprBinary) {
					this.visitExprBinary((ExprBinary) e);
				}
			}
			ltlWarning = false;
		}

		if (fName.equals("U") || fName.equals("R")) {
			if (func.getParams().size() != 2)
				throw new ExceptionAtNode("ill-formed LTL formula. 4", func);
		}

		if (ltlWarning && (fName.equals("F") || fName.equals("G") || fName.equals("X") || fName.equals("U")
				|| fName.equals("R"))) {
			// throw new ExceptionAtNode("ill-formed LTL formula. 5", func);
		}

		return super.visitExprFunCall(func);
	}

}