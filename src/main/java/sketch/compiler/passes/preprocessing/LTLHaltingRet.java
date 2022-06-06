package sketch.compiler.passes.preprocessing;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.exprs.ExprArrayRange;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssert;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtReturn;
import sketch.compiler.monitor.Graph;

public class LTLHaltingRet extends FEReplacer {

	Graph fa;

	public LTLHaltingRet(Graph fa) {
		this.fa = fa;
	}
	
	public Object visitStmtReturn(StmtReturn stmt) {
		FEContext curr = stmt.getCx();
		FEContext ncontext = new FEContext(curr.getFileName(), curr.getLineNumber(), curr.getColumnNumber(),
				curr.getComment());
		ncontext.setLTL(true);
		StmtAssign halting = new StmtAssign(ncontext, new ExprVar(ncontext, "h" + fa.getIdA()), new ExprConstInt(1));
		List<Statement> newStmts = createRegression(halting);
		this.addStatements(newStmts);
		this.addStatement(halting);
		Iterator<Statement> it = newStmts.iterator();
		while (it.hasNext()) {
			this.addStatement(it.next());
		}
		ExprConstInt finFA = new ExprConstInt(ncontext, fa.getFinalS().get(0));
		ExprArrayRange finAssert = new ExprArrayRange(new ExprVar(ncontext, "st" + fa.getIdA()), finFA);
		StmtAssert finAssertS = new StmtAssert(ncontext, finAssert, false);
		this.addStatement(finAssertS);

		this.addStatement(stmt);

		return null;
	}

	public List<Statement> createRegression(FENode context) {

		List<Statement> halting = new LinkedList<Statement>();

		for (int u = 0; u < fa.getV(); u++) {
			halting.add(fa.makeRegression(context, u));
		}

		FEContext curr = context.getCx();

		FEContext ncontext = new FEContext(curr.getFileName(), curr.getLineNumber(), curr.getColumnNumber(),
				curr.getComment());
		ncontext.setLTL(true);
		StmtAssign stCopy = new StmtAssign(ncontext, new ExprVar(context, "stc" + fa.getIdA()),
				new ExprVar(context, "st" + fa.getIdA()));
		halting.add(stCopy);

		return halting;

	}

}
