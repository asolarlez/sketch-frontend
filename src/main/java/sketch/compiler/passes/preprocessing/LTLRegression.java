package sketch.compiler.passes.preprocessing;

import java.util.Iterator;
import java.util.List;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.monitor.Graph;
import sketch.compiler.passes.lowering.MakeBodiesBlocks;

public class LTLRegression extends FEReplacer {

	private Graph fa;
	private List<Integer> ltlAsserts;

	public LTLRegression(List<Integer> ltlAsserts) {
		this.ltlAsserts = ltlAsserts;
	}

	public Object visitFunction(Function func) {
		Statement body = func.getBody();

		Iterator<Integer> it = ltlAsserts.iterator();

		while (it.hasNext()) {
			int ltlCurrentLine = it.next();

			LTL2BAFormat stringFormat = new LTL2BAFormat(ltlCurrentLine);
			body.accept(stringFormat);
			if (!stringFormat.ltlString().equals("")) {
				// Create the FA
				Graph LTLFA = new Graph(stringFormat.ltlString(), ltlCurrentLine);
				Graph LTLFA2 = LTLFA.finiteExc();
				LTLFA2.castAdj(body, stringFormat.getPropTable());
				boolean hasLTL = false;
				LTLStmtRegressions regressions = new LTLStmtRegressions(hasLTL, LTLFA2, ltlCurrentLine);
				body = (Statement) body.accept(regressions);
				hasLTL = regressions.getHasLTL();

				if (hasLTL) {
					body = (Statement) body.accept(new LTLHaltingRet(LTLFA2));
					body = (Statement) body.accept(new MakeBodiesBlocks());
					body = (Statement) body.accept(new LTLHalting(LTLFA2));
				}
			}
		}

		return func.creator().body((Statement) body).create();
	}

}
