/**
 *
 */
package sketch.compiler.passes.lowering;
import java.util.ArrayList;
import java.util.List;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.ExprBinary;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprConstant;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.stmts.StmtDoWhile;
import sketch.compiler.ast.core.stmts.StmtFor;
import sketch.compiler.ast.core.stmts.StmtLoop;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.stmts.StmtWhile;
import sketch.compiler.ast.core.typs.TypePrimitive;

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

        addStatement(new StmtVarDecl(stmt, TypePrimitive.bittype, first, null));
		addStatement (new StmtAssign (new ExprVar (stmt, first),
 ExprConstInt.one));
		List<Statement> newStmts = new ArrayList<Statement> (oldBody.getStmts ());
		newStmts.add (new StmtAssign (new ExprVar (stmt, first),
 ExprConstInt.zero));
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
