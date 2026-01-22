package sketch.compiler.passes.preprocessing;

import java.util.ArrayList;
import java.util.List;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssert;

public class CheckWithHelp extends FEReplacer {

	List<StmtAssert> ltlAsserts, assertsWH;

	public CheckWithHelp(List<StmtAssert> ltlAsserts) {
		this.ltlAsserts = ltlAsserts;
		assertsWH = new ArrayList<>();
	}

	public Object visitFunction(Function func) {
		if (func.isUninterp())
			return func;
		Statement body = func.getBody();
		IsWithHelp wh = new IsWithHelp();
		body.accept(wh);
		if (!wh.isWH)
			return super.visitFunction(func);

		return super.visitFunction(func);
	}

	private void filterWithHelps(List<StmtAssert> ltlAsserts) {
		for (StmtAssert stmt : ltlAsserts) {
			Expression cond = stmt.getCond();
			IsWithHelp wh = new IsWithHelp();
			cond.accept(wh);
			if (wh.isWH) {
				assertsWH.add(stmt);
			}
		}
	}

	class IsWithHelp extends FEReplacer {

		boolean isWH;

		IsWithHelp() {
			isWH = false;
		}

		public Object visitExprFunCall(ExprFunCall funCall) {
			isWH = funCall.getName().equals("with_help");
			// if (isWH)
			// System.out.println("test " + funCall);
			return super.visitExprFunCall(funCall);
		}

		public boolean getIsWH() {
			return isWH;
		}

	}

}
