package sketch.compiler.passes.preprocessing;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.util.exceptions.ExceptionAtNode;

public class LTLInAssert extends FEReplacer {

	public LTLInAssert() {
	}

	public Object visitExprFunCall(ExprFunCall func) throws ExceptionAtNode {

		String fName = func.getName();

		if (fName.equals("X") || fName.equals("F") || fName.equals("G") || fName.equals("U") || fName.equals("R"))
			throw new ExceptionAtNode("ltl formulas can only ocurr iniside of an assert.", func);

		return func;
	}

}
