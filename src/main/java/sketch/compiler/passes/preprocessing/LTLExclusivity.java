package sketch.compiler.passes.preprocessing;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.util.exceptions.ExceptionAtNode;

/**
 * Front-end visitor pass for verifying that function is named as an LTL
 * operator.
 * 
 * @author Fernando A. Galicia-Mendoza &lt;fmendoza@mit.edu&gt;
 * @version $Id$
 *
 */
public class LTLExclusivity extends FEReplacer {

	/**
	 * LTL operators only can be used as function calls.
	 */
	@Override
	public Object visitFunction(Function func) throws ExceptionAtNode {
		String fName = func.getName();
		if (fName.equals("X") || fName.equals("F") || fName.equals("G") || fName.equals("U") || fName.equals("Forall")
				|| fName.equals("Exists") || fName.equals("with_help"))
			throw new ExceptionAtNode("LTL operators cannot be used as function declarations.", func);
		return super.visitFunction(func);
	}

}
