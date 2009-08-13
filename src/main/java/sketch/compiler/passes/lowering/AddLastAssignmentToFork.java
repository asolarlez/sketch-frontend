package sketch.compiler.passes.lowering;
import java.util.ArrayList;
import java.util.List;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.ast.promela.stmts.StmtFork;

public class AddLastAssignmentToFork extends FEReplacer {
	public static final String PLACEHOLDER = "_END_";

	@Override
	public Object visitStmtFork(StmtFork stmt){

		Statement svd = new StmtVarDecl(stmt, TypePrimitive.inttype, PLACEHOLDER, ExprConstInt.zero);

		addStatement(svd);

		Statement fas = new StmtAssign(new ExprVar(stmt, PLACEHOLDER), ExprConstInt.zero);

		List<Statement> blist = new ArrayList<Statement>();
		blist.add(fas);
		blist.add(stmt.getBody());
		blist.add(fas);



		StmtFork nfork = new StmtFork(stmt, stmt.getLoopVarDecl(), stmt.getIter(), new StmtBlock(stmt.getBody(), blist));
		return nfork;
	}
}
