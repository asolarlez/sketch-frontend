/**
 *
 */
package sketch.compiler.passes.lowering;
import java.util.ArrayList;
import java.util.List;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.stmts.StmtFor;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.promela.stmts.StmtFork;

/**
 * Hoists declarations to the beginning of their enclosing scope.  This means
 * different things for different states of the AST; this pass assumes that:
*
 *   - variable names are unique
 *   - initializers have been separated
 *   - a function is an enclosing scope
 *   - a 'fork' is an enclosing scope
 *
 *
 * @author <a href="mailto:cgjones@cs.berkeley.edu">Chris Jones</a>
 */
public class HoistDeclarations extends FEReplacer {
	List<StmtVarDecl> declList = null;

	public Object visitFunction (Function f) {
		assert null == declList;

		declList = new ArrayList<StmtVarDecl> ();
		Function newF = (Function) super.visitFunction (f);

		if (f != newF) {
			List<Statement> newBody =
				new ArrayList<Statement> (((StmtBlock) newF.getBody ()).getStmts ());
			newBody.addAll (0, declList);
            newF = newF.creator().body(new StmtBlock(newF.getBody(), newBody)).create();
		}

		declList = null;
		return newF;
	}

	public Object visitStmtFork (StmtFork sf) {
		List<StmtVarDecl> oldScope = declList;
		declList = new ArrayList<StmtVarDecl> ();

		StmtBlock oldBody = (StmtBlock) sf.getBody ();
		StmtBlock newBody = (StmtBlock) oldBody.accept (this);

		StmtFork newFork = null;
		if (newBody == oldBody) {
			newFork = sf;
		} else {
			List<Statement> newBodyStmts = new ArrayList<Statement> (newBody.getStmts ());
			newBodyStmts.addAll (0, declList);
			newFork = new StmtFork (sf, sf.getLoopVarDecl (), sf.getIter (),
					new StmtBlock (newBody, newBodyStmts));
		}

		declList = oldScope;
		return newFork;
	}

	public Object visitStmtFor (StmtFor sf) {
		Statement oldBody = sf.getBody ();
		Statement newBody = (Statement) oldBody.accept (this);

		return (newBody == oldBody) ? sf :
			new StmtFor (sf, sf.getInit (), sf.getCond (), sf.getIncr (), newBody);
	}

	public Object visitStmtVarDecl (StmtVarDecl svd) {
		for (Expression e : (List<Expression>) svd.getInits ())
			assert null == e;
		declList.add (svd);
		return null;
	}
}
