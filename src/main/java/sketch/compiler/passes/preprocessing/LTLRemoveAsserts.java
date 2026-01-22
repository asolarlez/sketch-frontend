package sketch.compiler.passes.preprocessing;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.stmts.StmtAssert;
import sketch.compiler.ast.core.stmts.StmtAssume;

/**
 * Front-end visitor pass that removes all the asserts and assumes.
 * 
 * @author Fernando Abigail Galicia-Mendoza &lt;fmendoza@mit.edu&gt;
 * @version $Id$
 *
 */
public class LTLRemoveAsserts extends FEReplacer {

	public Object visitStmtAssert(StmtAssert stmt) {
		return null;
	}

	public Object visitStmtAssume(StmtAssume stmt) {
		return null;
	}

}
