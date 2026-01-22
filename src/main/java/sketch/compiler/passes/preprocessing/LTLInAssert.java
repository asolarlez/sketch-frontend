package sketch.compiler.passes.preprocessing;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.util.exceptions.ExceptionAtNode;

/**
 * Front-end visitor pass for verifying that the LTL formulae occur only in
 * assert statements.
 * 
 * @author Fernando Abigail Galicia-Mendoza &lt;fmendoza@mit.edu&gt;
 * @version $Id$
 *
 */
public class LTLInAssert extends FEReplacer {

	public LTLInAssert() {
	}

	public Object visitExprFunCall(ExprFunCall func) throws ExceptionAtNode {

		String fName = func.getName();

		if (fName.equals("X") || fName.equals("F") || fName.equals("G") || fName.equals("U") || fName.equals("Forall")
				|| fName.equals("Exists") || fName.equals("with_help"))
			throw new ExceptionAtNode("LTL formulas ocurr exclusively in assert statements.", func);

		return func;
	}

}
