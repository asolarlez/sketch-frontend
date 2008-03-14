/**
 *
 */
package streamit.frontend.passes;

import java.util.ArrayList;
import java.util.List;

import streamit.frontend.nodes.ExprBinary;
import streamit.frontend.nodes.ExprConstBoolean;
import streamit.frontend.nodes.ExprConstant;
import streamit.frontend.nodes.ExprVar;
import streamit.frontend.nodes.Expression;
import streamit.frontend.nodes.FEReplacer;
import streamit.frontend.nodes.Statement;
import streamit.frontend.nodes.StmtAssign;
import streamit.frontend.nodes.StmtBlock;
import streamit.frontend.nodes.StmtDoWhile;
import streamit.frontend.nodes.StmtFor;
import streamit.frontend.nodes.StmtLoop;
import streamit.frontend.nodes.StmtVarDecl;
import streamit.frontend.nodes.StmtWhile;
import streamit.frontend.nodes.TempVarGen;
import streamit.frontend.nodes.TypePrimitive;

/**
 * As the name says, convert all loops into while loops.
 *
 * The conversion assumes that there are no 'continue' statements within the
 * loops.
 *
 * @author <a href="mailto:cgjones@cs.berkeley.edu">Chris Jones</a>
 */
public class LowerLoopsToWhileLoops extends FEReplacer {
	protected TempVarGen varGen;

	public LowerLoopsToWhileLoops (TempVarGen _varGen) {
		varGen = _varGen;
	}

	public Object visitStmtDoWhile (StmtDoWhile stmt) {
		StmtBlock oldBody = (StmtBlock) stmt.getBody ().accept (this);
		Expression oldCond = (Expression) stmt.getCond ().accept (this);
		String first = varGen.nextVar ("do_while_first_iter");

		addStatement (new StmtVarDecl (stmt, TypePrimitive.booltype, first, null));
		addStatement (new StmtAssign (new ExprVar (stmt, first),
				      new ExprConstBoolean (stmt, true)));
		List<Statement> newStmts = new ArrayList<Statement> (oldBody.getStmts ());
		newStmts.add (new StmtAssign (new ExprVar (stmt, first),
									  new ExprConstBoolean (stmt, false)));
		StmtBlock newBody = new StmtBlock (stmt.getBody (), newStmts);
		Expression newCond = new ExprBinary (
				new ExprVar (oldCond, first), "||", oldCond);

		return new StmtWhile (stmt, newCond, newBody);
	}

	public Object visitStmtFor (StmtFor stmt) {
		StmtBlock oldBody = (StmtBlock) stmt.getBody ().accept (this);
		Expression oldCond = (Expression) stmt.getCond ().accept (this);

		addStatement ((Statement) stmt.getInit ().accept (this));
		List<Statement> newStmts = new ArrayList<Statement> (oldBody.getStmts ());
		newStmts.add (stmt.getIncr ());
		StmtBlock newBody = new StmtBlock (oldBody, newStmts);

		return new StmtWhile (stmt, oldCond, newBody);
	}

	public Object visitStmtLoop (StmtLoop stmt) {
		StmtBlock oldBody = (StmtBlock) stmt.getBody ().accept (this);
		String loopVarName = varGen.nextVar ("loop_iter");
		ExprVar loopVar = new ExprVar (stmt, loopVarName);

		addStatement (new StmtVarDecl (stmt, TypePrimitive.inttype, loopVarName, null));
		addStatement (new StmtAssign (loopVar,
									  ExprConstant.createConstant (loopVar, "0")));
		List<Statement> newStmts = new ArrayList<Statement> (oldBody.getStmts ());
		newStmts.add (new StmtAssign (
						loopVar,
						new ExprBinary (loopVar, "+",
										ExprConstant.createConstant (loopVar, "1"))));
		StmtBlock newBody = new StmtBlock (oldBody, newStmts);
		Expression cond = new ExprBinary (loopVar, "<", stmt.getIter ());

		return new StmtWhile (stmt, cond, newBody);
	}
}
