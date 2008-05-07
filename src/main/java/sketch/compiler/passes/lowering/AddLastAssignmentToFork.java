package streamit.frontend.passes;

import java.util.ArrayList;
import java.util.List;

import streamit.frontend.nodes.ExprConstInt;
import streamit.frontend.nodes.ExprVar;
import streamit.frontend.nodes.FEReplacer;
import streamit.frontend.nodes.Statement;
import streamit.frontend.nodes.StmtAssign;
import streamit.frontend.nodes.StmtBlock;
import streamit.frontend.nodes.StmtFork;
import streamit.frontend.nodes.StmtVarDecl;
import streamit.frontend.nodes.TypePrimitive;

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
