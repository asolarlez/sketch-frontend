/**
 *
 */
package sketch.compiler.passes.lowering;
import java.util.Collections;
import java.util.List;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.SymbolTable;
import sketch.compiler.ast.core.exprs.ExprBinary;
import sketch.compiler.ast.core.exprs.ExprTernary;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.exprs.regens.ExprRegen;
import sketch.compiler.ast.core.stmts.*;
import sketch.compiler.ast.promela.stmts.StmtFork;
import sketch.util.ControlFlowException;

/**
 * Prepares an AST for later rewrite steps that might turn a single
 * statement into multiple ones.  For example, if the user writes this
 * program:
 *
 * <code>
 * reorder {
 *   a = b && c;
 *   x = y && z;
 * }
 * </code>
 *
 * The eventual rewrite will look something like:
 *
 * <code>
 * reorder {
 *   tmp1 = b;
 *   if (tmp1)  tmp1 = c;
 *   tmp2 = y;
 *   if (tmp2)  tmp2 = z;
 * }
 * </code>
 *
 * However, the 'reorder' block now has 4 statements instead of 2, which was
 * not the user's intention.  To solve this problem, this pass will convert
 * the original code into this format:
 *
 * <code>
 * reorder {
 *   { a = b && c; }
 *   { x = y && z; }
 * }
 * </code>
 *
 * This assumes that initializers have been separated out from variable
 * declarations.
 *
 * @author <a href="mailto:cgjones@cs.berkeley.edu">Chris Jones</a>
 */
public class BlockifyRewriteableStmts extends SymbolTableVisitor {
	public BlockifyRewriteableStmts () {
		super (null);
	}

	public Object visitStmtAssert (StmtAssert stmt) {
		return isRewriteable (stmt.getCond ()) ? blockify (stmt) : stmt;
	}

	public Object visitStmtAssign (StmtAssign stmt) {
		return isRewriteable (stmt.getLHS ()) || isRewriteable (stmt.getRHS ()) ?
				blockify (stmt) : stmt;
	}

	public Object visitStmtAtomicBlock (StmtAtomicBlock stmt) {
		stmt = (StmtAtomicBlock) super.visitStmtAtomicBlock (stmt);
		return stmt.isCond () && isRewriteable (stmt.getCond ()) ?
				blockify (stmt) : stmt;
	}

	public Object visitStmtDoWhile (StmtDoWhile stmt) {
		return blockify ((Statement) super.visitStmtDoWhile (stmt));
	}

	public Object visitStmtExpr (StmtExpr stmt) {
		return isRewriteable (stmt.getExpression ()) ? blockify (stmt) : stmt;
	}

	protected boolean enforceSeparatedInits = true;
	public Object visitStmtFor (StmtFor stmt) {
		SymbolTable oldSymtab = symtab;
		symtab = new SymbolTable (symtab);

		enforceSeparatedInits = false;
		stmt.getInit ().accept (this);
		stmt.getIncr ().accept (this);
		enforceSeparatedInits = true;
		// To make life easier, we just always blockify 'for' statements.
		Statement newBody = (Statement) stmt.getBody ().accept (this);

		symtab = oldSymtab;
		return blockify (new StmtFor (stmt,
				stmt.getInit (), stmt.getCond (), stmt.getIncr (), newBody));
	}

	public Object visitStmtFork (StmtFork stmt) {
		super.visitStmtFork (stmt);
		// To make life easier, we just always blockify 'fork' statements.
		return blockify ((Statement) super.visitStmtFork (stmt));
	}

	public Object visitStmtIfThen (StmtIfThen stmt) {
		stmt = (StmtIfThen) super.visitStmtIfThen (stmt);
		return isRewriteable (stmt.getCond ()) ? blockify (stmt) : stmt;
	}

	public Object visitStmtLoop (StmtLoop stmt) {
		stmt = (StmtLoop) super.visitStmtLoop (stmt);
		return isRewriteable (stmt.getIter ()) ? blockify (stmt) : stmt;
	}

	public Object visitStmtReturn (StmtReturn stmt) {
		stmt = (StmtReturn) super.visitStmtReturn (stmt);
		return isRewriteable (stmt.getValue ()) ? blockify (stmt) : stmt;
	}

	public Object visitStmtVarDecl (StmtVarDecl stmt) {
		super.visitStmtVarDecl (stmt);
		if (!enforceSeparatedInits)  return stmt;
		for (Expression e : (List<Expression>) stmt.getInits ())
            stmt.assertTrue(null == e,
                    "Should have run separate initializers before this");
		return stmt;
	}

	public Object visitStmtWhile (StmtWhile stmt) {
		return blockify ((Statement) super.visitStmtWhile (stmt));
	}

	protected StmtBlock blockify (Statement s) {
		return new StmtBlock (s, Collections.singletonList (s));
	}

	protected boolean isRewriteable (Expression e) {
		return null != e
			&& (e instanceof ExprRegen
				|| isGlobal (e)
				|| hasShortCircuit (e));
	}

	protected boolean hasShortCircuit (Expression e) {
		class checker extends FEReplacer {
			public Object visitExprBinary (ExprBinary eb) {
				String op = eb.getOpString ();
				if ("&&".equals (op) || "||".equals (op))
					throw new ControlFlowException ("yes");
				return super.visitExprBinary (eb);
    		}
			public Object visitExprTernary (ExprTernary et) {
				throw new ControlFlowException ("yes");
			}
    	}
		try {  e.accept (new checker ());  return false;  }
		catch (ControlFlowException re) {  return true;
		}
	}
}
