package sketch.compiler.passes.preprocessing;

import java.util.List;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.util.exceptions.ExceptionAtNode;

/**
 * Front-end visitor pass for verifying that the LTL expressions are
 * syntactically well-formed.
 * 
 * @author Fernando A. Galicia-Mendoza &lt;fmendoza@mit.edu&gt;
 * @version $Id$
 *
 */
public class LTLWFExpression extends FEReplacer {

	public Object visitExprFunCall(ExprFunCall func) throws ExceptionAtNode {
		String fname = func.getName();
		List<Expression> params = func.getParams();
		if ((fname.equals("X") || fname.equals("F") || fname.equals("G")) && params.size() != 1) {
			throw new ExceptionAtNode(fname + " operator is unary.", func);
		}
		if (fname.equals("U") && params.size() != 2) {
			throw new ExceptionAtNode("U operator is binary.", func);
		}
		if ((fname.equals("Forall") || fname.equals("Exists")) && params.size() != 3) {
			throw new ExceptionAtNode(fname + " cuantifier is ternary.", func);
		}
		if ((fname.equals("with_help"))) {
			if (params.size() == 2) {
				Expression form = params.get(0);
				form.accept(new CheckLTL());
			} else
				throw new ExceptionAtNode("with_help needs two parameters.", func);
		}
		return super.visitExprFunCall(func);
	}

	class CheckLTL extends FEReplacer {
		public Object visitExprFunCall(ExprFunCall func) throws ExceptionAtNode {
			String fname = func.getName();
			List<Expression> params = func.getParams();
			if ((fname.equals("X") || fname.equals("F") || fname.equals("G")) && params.size() != 1) {
				throw new ExceptionAtNode(fname + " operator is unary.", func);
			}
			if (fname.equals("U") && params.size() != 2) {
				throw new ExceptionAtNode("U operator is binary.", func);
			}
			if ((fname.equals("Forall") || fname.equals("Exists")) && params.size() != 3) {
				throw new ExceptionAtNode(fname + " cuantifier is ternary.", func);
			}
			return super.visitExprFunCall(func);
		}
	}

}