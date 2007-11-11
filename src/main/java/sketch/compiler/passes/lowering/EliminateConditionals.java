/**
 *
 */
package streamit.frontend.passes;

import streamit.frontend.nodes.ExprTernary;
import streamit.frontend.nodes.ExprVar;
import streamit.frontend.nodes.StmtAssign;
import streamit.frontend.nodes.StmtIfThen;
import streamit.frontend.nodes.StmtVarDecl;
import streamit.frontend.nodes.TempVarGen;

/**
 * @author higgins
 *
 */
public class EliminateConditionals extends SymbolTableVisitor {
	protected TempVarGen varGen;

	/**
	 * @param symtab
	 */
	public EliminateConditionals (TempVarGen _vargen) {
		super (null);
		varGen = _vargen;
	}

	public Object visitExprTernary (ExprTernary et) {
		ExprVar tmpVar =
			new ExprVar (et.getCx (), varGen.nextVar ("_tmp_ternary_elim_"));
		StmtVarDecl tmpDecl =
			new StmtVarDecl (et.getCx (), getType (et), tmpVar.getName (), null);
		StmtAssign thenAssn = new StmtAssign (et.getCx (), tmpVar, et.getB ());
		StmtAssign elseAssn = new StmtAssign (et.getCx (), tmpVar, et.getC ());
		StmtIfThen condReplace =
			new StmtIfThen (et.getCx (), et.getA (), thenAssn, elseAssn);

		addStatement (tmpDecl);
		addStatement (condReplace);

		return tmpVar;
	}
}
