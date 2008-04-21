/**
 *
 */
package streamit.frontend.passes;

import java.util.ArrayList;
import java.util.List;

import streamit.frontend.nodes.ExprTernary;
import streamit.frontend.nodes.ExprVar;
import streamit.frontend.nodes.Expression;
import streamit.frontend.nodes.FENode;
import streamit.frontend.nodes.Statement;
import streamit.frontend.nodes.StmtAssign;
import streamit.frontend.nodes.StmtBlock;
import streamit.frontend.nodes.StmtIfThen;
import streamit.frontend.nodes.StmtVarDecl;
import streamit.frontend.nodes.TempVarGen;
import streamit.frontend.nodes.Type;
import streamit.frontend.nodes.TypePrimitive;

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
		if (1 >= ExprTools.numGlobalReads (et, symtab))
			return et;

		FENode cx = et;
		ExprVar tmpVar =
			new ExprVar (cx, varGen.nextVar ("_tmp_cond_elim_"));
		Type t = getType (et, TypePrimitive.inttype);

		StmtVarDecl tmpDecl =
			new StmtVarDecl (cx, t, tmpVar.getName (), t.defaultValue ());

		List<Statement> oldStatements = newStatements;

		newStatements = new ArrayList<Statement> ();
		newStatements.add (
				new StmtAssign (tmpVar, (Expression)et.getB ().accept (this)));
		Statement thenBlock = new StmtBlock (cx, newStatements);

		newStatements = new ArrayList<Statement> ();
		newStatements.add (
				new StmtAssign (tmpVar, (Expression)et.getC ().accept (this)));
		Statement elseBlock = new StmtBlock (cx, newStatements);

		newStatements = oldStatements;

		StmtIfThen condReplace =
			new StmtIfThen (cx, (Expression) et.getA ().accept (this),
							thenBlock, elseBlock);

		addStatement (tmpDecl);
		addStatement (condReplace);

		return tmpVar;
	}
}
