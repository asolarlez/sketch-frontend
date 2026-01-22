package sketch.compiler.main.seq;

import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.StmtReturn;
import sketch.compiler.ast.core.stmts.StmtWhile;

public class LTLWhileForever extends FEReplacer {

	boolean hasRet;
	boolean isInf;
	FEContext context;
	StmtWhile forever;

	public LTLWhileForever() {
		hasRet = false;
		isInf = false;
		context = null;
		forever = null;
	}

	public Object visitStmtWhile(StmtWhile loop) {
		loop.accept(new BodyLoopRet());
		Expression cond = (Expression) loop.getCond();
		isInf = cond.toString().equals("1");
		forever = isInf ? loop : null;
		context = loop.getCx();
		return loop;
	}

	public boolean getHasRet() {
		return hasRet;
	}

	public boolean getIsInf() {
		return isInf;
	}

	public Object getContext() {
		return context;
	}

	public Object getForever() {
		return forever;
	}

	class BodyLoopRet extends FEReplacer {

		public Object visitStmtReturn(StmtReturn ret) {
			hasRet = true;
			return ret;
		}
	}
}
