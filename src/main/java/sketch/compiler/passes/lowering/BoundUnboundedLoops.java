/**
 *
 */
package sketch.compiler.passes.lowering;
import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.ExprBinary;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprUnary;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.*;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypePrimitive;

/**
 * Converts unbounded 'for', 'while', and 'do-while' loops into a bounded form.
 *
 * For example:
 * <code>
 *   while (cond) {
 *     ...
 *   }
 *   // transformed to
 *   bit terminated = 0;
 *   for (int i = 0; i < maxIterations; ++i) {
 *     if (cond) {
 *       ...
 *     } else {
 *       terminated = 1;
 *     }
 *   }
 *   assert terminated;
 * </code>
 *
 * @author <a href="mailto:cgjones@cs.berkeley.edu">Chris Jones</a>
 */
public class BoundUnboundedLoops extends FEReplacer {
	private static final String NONTERM_MESSAGE =
		"either not enough '--unrollamnt', or infinite loop";

	private TempVarGen varGen;
	private Expression maxIterations;

	public BoundUnboundedLoops (TempVarGen varGen, int maxIterations) {
		this.varGen = varGen;
		this.maxIterations = new ExprConstInt (maxIterations);
	}

	public Object visitStmtDoWhile (StmtDoWhile stmt) {
		Statement body = (Statement) stmt.getBody ().accept (this);
		Expression cond = doExpression (stmt.getCond ());
		ExprVar first = makeVar (stmt, TypePrimitive.bittype, "_firstiter", ExprConstInt.one);
		Statement newLoopBody = new StmtBlock (stmt,
				body,
				new StmtAssign (first, ExprConstInt.zero));
		Expression newCond = new ExprBinary (
			new ExprBinary (first, "==", ExprConstInt.one),
			"||",
			cond);
		return makeBoundedLoop (stmt, newCond, newLoopBody);
	}

	public Object visitStmtFor (StmtFor stmt) {
		if (isCanonicalForLoop (stmt))
			return super.visitStmtFor (stmt);

		Statement body = (Statement) stmt.getBody ().accept (this);
		Statement init = (Statement) stmt.getInit ().accept (this);
		Expression cond = doExpression (stmt.getCond ());
		Statement incr = (Statement) stmt.getIncr ().accept (this);
		StmtBlock newLoopBody = new StmtBlock (stmt, body, incr);

		// In a block to preserve scoping of 'init'
		return new StmtBlock (stmt, init, makeBoundedLoop (stmt, cond, newLoopBody));
	}

	public Object visitStmtWhile (StmtWhile stmt) {
		return makeBoundedLoop (stmt,
				doExpression (stmt.getCond ()),
				(Statement) stmt.getBody ().accept (this));
	}

	/** Canonical form is:  for (int i = CONST; i CMP CONST; (++|--)i(++|--)) */
	private boolean isCanonicalForLoop (StmtFor stmt) {
		
		return SimpleLoopUnroller.decideForLoop(stmt) >= 0;
		/*
		if (!(stmt.getInit () instanceof StmtVarDecl
			  && stmt.getCond () instanceof ExprBinary
			  && stmt.getIncr () instanceof StmtExpr))
			return false;

		StmtVarDecl init = (StmtVarDecl) stmt.getInit ();
		ExprBinary cond = (ExprBinary) stmt.getCond ();
		StmtExpr incr = (StmtExpr) stmt.getIncr ();
		if (!(init.getNumVars () == 1
			  && cond.isComparison ()
			  && incr.getExpression () instanceof ExprUnary))
			return false;

		String i = init.getName (0);
		Expression iVal = init.getInit (0);
		Expression cmpLeft = cond.getLeft ();
		Expression cmpRight = cond.getRight ();
		ExprUnary incrExp = (ExprUnary) incr.getExpression ();
		if (!(iVal.isConstant ()
			  && (cmpLeft.isConstant () || cmpRight.isConstant ())
			  && (cmpLeft instanceof ExprVar || cmpRight instanceof ExprVar)
			  && incrExp.isIncrOrDecr ()
			  && incrExp.getExpr () instanceof ExprVar))
			return false;

		ExprVar cmpVar = (ExprVar) ((cmpLeft instanceof ExprVar) ? cmpLeft : cmpRight);
		ExprVar incrVar = (ExprVar) incrExp.getExpr ();

		return i.equals (cmpVar.getName ()) && i.equals (incrVar.getName ());
		*/
	}

	private Statement makeBoundedLoop (FENode cx, Expression cond, Statement body) {
		ExprVar term = makeVar (cx, TypePrimitive.bittype, "_terminated", ExprConstInt.zero);
		String iterName = varGen.nextVar ("_i");
		ExprVar iter = new ExprVar (cx, iterName);

		Statement newBody = new StmtBlock (cx,
				new StmtIfThen (cx, cond, body,
						new StmtBlock (cx, new StmtAssign (term, ExprConstInt.one))));
		Statement wrappedBody = new StmtBlock (cx,
				new StmtIfThen (cx,
						new ExprBinary (term, "==", ExprConstInt.zero),
						newBody, null));
		Statement loop = new StmtFor (cx,
				new StmtVarDecl (cx, TypePrimitive.inttype, iterName, ExprConstInt.zero),
				new ExprBinary (iter, "<", maxIterations),
				new StmtExpr (new ExprUnary (cx, ExprUnary.UNOP_POSTINC, iter)),
				wrappedBody);

		return new StmtBlock (cx, loop, makeTerminatedAssertion (term));
	}

	private Statement makeTerminatedAssertion (ExprVar term) {
		return new StmtAssert (new ExprBinary (term, "==", ExprConstInt.one),
				NONTERM_MESSAGE, false);
	}

	private ExprVar makeVar (FENode cx, Type t, String pfx, Expression init) {
		String name = varGen.nextVar (pfx);
		addStatement (new StmtVarDecl (cx, t, name, init));
		return new ExprVar (cx, name);
	}
}
