package sketch.compiler.passes.preprocessing;

import java.util.LinkedList;
import java.util.List;

import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.exprs.ExprBinary;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprUnary;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.StmtAssert;

public class LTLPrefixFormat extends FEReplacer {

	public Object visitStmtAssert(StmtAssert stmt) {
		FEContext context = stmt.getCx();

		if (context.getLTLAssert()) {
			Expression cond = stmt.getCond();
			cond = (Expression) cond.accept(new PrefixBoolean());
			return new StmtAssert(context, cond, false);
		}

		return super.visitStmtAssert(stmt);
	}

	class PrefixBoolean extends FEReplacer {
		public Object visitExprBinary(ExprBinary bin) {

			Expression left = bin.getLeft();
			Expression right = bin.getRight();
			List<Expression> params = new LinkedList<Expression>();
			left = (Expression) left.accept(this);
			right = (Expression) right.accept(this);
			switch (bin.getOp()) {
			case ExprBinary.BINOP_AND:
				params.add(left);
				params.add(right);
				return new ExprFunCall(bin, "&&", params);
			case ExprBinary.BINOP_OR:
				params.add(left);
				params.add(right);
				return new ExprFunCall(bin, "||", params);
			}

			return super.visitExprBinary(bin);

		}

		public Object visitExprUnary(ExprUnary un) {

			Expression expr = un.getExpr();
			List<Expression> param = new LinkedList<Expression>();
			expr = (Expression) expr.accept(this);
			switch (un.getOp()) {
			case ExprUnary.UNOP_NOT:
				param.add(expr);
				return new ExprFunCall(un, "!", param);
			}

			return super.visitExprUnary(un);
		}

		public Object visitExprConstInt(ExprConstInt i) {

			switch (i.getVal()) {
			case 0:
				return new ExprVar(i, "false");
			case 1:
				return new ExprVar(i, "true");
			}

			return super.visitExprConstInt(i);
		}
	}

}
