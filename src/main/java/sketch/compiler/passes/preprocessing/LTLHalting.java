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
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.monitor.Graph;

public class LTLHalting extends FEReplacer {

	Graph fa;

	public LTLHalting(Graph fa) {
		this.fa = fa;
	}

	public Object visitStmtBlock(StmtBlock block) {
		List<Statement> stmts = block.getStmts();
		List<Statement> newStmts = new LinkedList<Statement>();

		Iterator<Statement> it = stmts.iterator();
		while (it.hasNext()) {
			newStmts.add(it.next());
		}

		FEContext curr = block.getCx();
		FEContext ncontext = new FEContext(curr.getFileName(), curr.getLineNumber(), curr.getColumnNumber(),
				curr.getComment());
		ncontext.setLTL(true);
		StmtAssign halting = new StmtAssign(ncontext, new ExprVar(ncontext, "h" + fa.getIdA()), new ExprConstInt(1));
		newStmts.addAll(createRegression(halting));
		newStmts.add(halting);
		newStmts.addAll(createRegression(halting));
		ExprConstInt finFA = new ExprConstInt(ncontext, fa.getFinalS().get(0));
		ExprArrayRange finAssert = new ExprArrayRange(new ExprVar(ncontext, "st" + fa.getIdA()), finFA);
		StmtAssert finAssertS = new StmtAssert(ncontext, finAssert, false);
		newStmts.add(finAssertS);

		return new StmtBlock(ncontext, newStmts);
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
