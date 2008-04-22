package streamit.frontend.passes;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import streamit.frontend.nodes.ExprArrayRange;
import streamit.frontend.nodes.ExprBinary;
import streamit.frontend.nodes.ExprConstInt;
import streamit.frontend.nodes.ExprStar;
import streamit.frontend.nodes.ExprUnary;
import streamit.frontend.nodes.ExprVar;
import streamit.frontend.nodes.Expression;
import streamit.frontend.nodes.FENode;
import streamit.frontend.nodes.FEReplacer;
import streamit.frontend.nodes.Statement;
import streamit.frontend.nodes.StmtExpr;
import streamit.frontend.nodes.StmtFor;
import streamit.frontend.nodes.StmtInsertBlock;
import streamit.frontend.nodes.StmtLoop;
import streamit.frontend.nodes.StmtReorderBlock;
import streamit.frontend.nodes.StmtAssert;
import streamit.frontend.nodes.StmtAssign;
import streamit.frontend.nodes.StmtBlock;
import streamit.frontend.nodes.StmtIfThen;
import streamit.frontend.nodes.StmtVarDecl;
import streamit.frontend.nodes.TempVarGen;
import streamit.frontend.nodes.TypeArray;
import streamit.frontend.nodes.TypePrimitive;
import streamit.misc.Pair;

/**
 *
 * Replaces the Anyorder nodes with basic integer holes.
 *
 * @author asolar
 *
 */
public class EliminateReorderBlocks extends FEReplacer {

	private boolean insertBlockRewrite;
	private TempVarGen varGen;

	public EliminateReorderBlocks(TempVarGen varGen){
		this (varGen, false);
	}

	public EliminateReorderBlocks (TempVarGen varGen, boolean insertBlockRewrite) {
		this.varGen = varGen;
		this.insertBlockRewrite = insertBlockRewrite;
	}

	public Object visitStmtReorderBlock (StmtReorderBlock stmt) {
		return insertBlockRewrite ? lowerToInsertBlocks (stmt)
				: lowerToSwitchedLoop (stmt);
	}

	protected Statement lowerToInsertBlocks (StmtReorderBlock srb) {
		List<Statement> B = srb.getStmts ();

		if (1 >= B.size ())
			return (Statement) srb.getBlock ().accept (this);	// no reorder for 1 stmt

		StmtBlock into = new StmtBlock (srb,
				Collections.singletonList ((Statement)
						(new StmtReorderBlock (srb, B.subList (1, B.size ()))).accept (this)));
		Statement insert = (Statement) B.get (0).accept (this);

		return new StmtInsertBlock (srb, insert, into);
	}

	public Statement lowerToSwitchedLoop (StmtReorderBlock stmt) {
		FENode cx = stmt;
		String pickedName = varGen.nextVar ("_stmtPicked_");
		String choicesName = varGen.nextVar ("_regenChoices_");
		Expression len = new ExprConstInt (cx, stmt.getStmts ().size ());
		StmtVarDecl pickedDecl =
			new StmtVarDecl (cx, new TypeArray (TypePrimitive.bittype, len),
					pickedName, ExprConstInt.zero);
		StmtVarDecl choicesDecl =
			new StmtVarDecl (cx, new TypeArray (TypePrimitive.inttype, len),
					choicesName, ExprConstInt.zero);
		ExprVar picked = new ExprVar (cx, pickedName);
		ExprVar choices = new ExprVar (cx, choicesName);

		// Pick and verify a statement order
		String chooseIterName = varGen.nextVar ("_ic_");
		String execIterName = varGen.nextVar ("_ix_");
		StmtVarDecl chooseIterDecl =
			new StmtVarDecl (cx, TypePrimitive.inttype, chooseIterName, ExprConstInt.zero);
		StmtVarDecl execIterDecl =
			new StmtVarDecl (cx, TypePrimitive.inttype, execIterName, ExprConstInt.zero);
		ExprVar chooseIter = new ExprVar (cx, chooseIterName);
		ExprVar execIter = new ExprVar (cx, execIterName);
		Pair<Statement, Statement> chooseExec = makeChoiceAndExecStmts (
				stmt.getStmts ().iterator (), 0,
				chooseIter, execIter, picked, choices);
		StmtLoop choiceLoop = new StmtLoop (cx, len,
				new StmtBlock (cx,
						chooseExec.getFirst (),
						new StmtExpr (cx, new ExprUnary (cx, ExprUnary.UNOP_PREINC, chooseIter))));
		Expression valid = new ExprArrayRange (cx, picked, ExprConstInt.zero);
		for (int j = 1; j < stmt.getStmts ().size (); ++j)
			valid = new ExprBinary (
						valid,
						"&&",
						new ExprArrayRange (cx, picked, new ExprConstInt (cx, j)));
		StmtAssert checkChoices = new StmtAssert (valid, "bad stmt order");

		// Execute the statements
		StmtFor execLoop = new StmtFor (cx,
				execIterDecl,
				new ExprBinary (execIter, "<", len),
				new StmtExpr (new ExprUnary (stmt, ExprUnary.UNOP_PREINC, execIter)),
				new StmtBlock (cx, chooseExec.getSecond ()));

		return new StmtBlock (cx, pickedDecl, choicesDecl, chooseIterDecl,
				choiceLoop, checkChoices, execLoop);
	}

	/** Make the statements that (1) choose a statement to execute at a
	 * particular execution step, and (2) execute that chosen statement. */
	private Pair<Statement, Statement>
	makeChoiceAndExecStmts (Iterator<Statement> it, int i,
			Expression chooseIter, Expression execIter,
			Expression picked, Expression choices)
	{
		if (!it.hasNext ())
			return new Pair<Statement, Statement> (null, null);

		Statement s = it.next ();
		FENode cx = s;
		Expression me = new ExprConstInt (cx, i);
		Expression notPicked = new ExprUnary ("!", new ExprArrayRange (cx, picked, me));
		Expression pickMe = !it.hasNext () ? notPicked
				: new ExprBinary (notPicked, "&&", new ExprStar (cx));
		Statement setPicked = new StmtAssign (new ExprArrayRange (cx, picked, me), ExprConstInt.one);
		Statement setChoice = new StmtAssign (new ExprArrayRange (cx, choices, me), chooseIter);
		Expression myTurn = new ExprBinary (
				new ExprArrayRange (cx, choices, execIter), "==", me);

		Pair<Statement, Statement> recurse =
			makeChoiceAndExecStmts (it, i + 1, chooseIter, execIter, picked, choices);
		return new Pair<Statement, Statement> (
				new StmtIfThen (cx, pickMe, new StmtBlock (cx, setPicked, setChoice), recurse.getFirst ()),
				new StmtIfThen (cx, myTurn, new StmtBlock (cx, s), recurse.getSecond ()));
	}
}