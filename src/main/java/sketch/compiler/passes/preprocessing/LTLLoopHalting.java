package sketch.compiler.passes.preprocessing;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.monitor.Graph;

public class LTLLoopHalting extends FEReplacer {

	Graph fa;

	public LTLLoopHalting(Graph fa) {
		this.fa = fa;
	}

	public Object visitStmtBlock(StmtBlock block) {
		List<Statement> stmts = block.getStmts();
		List<Statement> newStmts = new LinkedList<Statement>();

		Iterator<Statement> it = stmts.iterator();
		
		while (it.hasNext()) {
			Statement si = it.next();
			if (!it.hasNext()) {
				si = (Statement) si.accept(new LTLHalting(fa));
				newStmts.add(si);
			}
		}
		return new StmtBlock(block.getCx(), newStmts.get(0));
	}

}
