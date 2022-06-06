package sketch.compiler.passes.preprocessing;

import java.util.LinkedList;
import java.util.List;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.exprs.ExprArrayRange;
import sketch.compiler.ast.core.exprs.ExprArrayRange.RangeLen;
import sketch.compiler.ast.core.exprs.ExprBinary;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.passes.lowering.SymbolTableVisitor;

public class LTLProtectExprs extends SymbolTableVisitor {

	public LTLProtectExprs() {
		super(null);
	}

	public Object visitStmtAssign(StmtAssign stmt) {
		if (stmt.getCx().getLTL()) {
			Expression left = stmt.getLHS();
			Expression right = stmt.getRHS();
			right = (Expression) right.accept(new ProtectCondAsserts());
			return new StmtAssign(stmt, left, right);
		}

		return super.visitStmtAssign(stmt);

	}

	class ProtectCondAsserts extends FEReplacer {

		public Object visitExprBinary(ExprBinary bin) {
			Expression left = bin.getLeft();
			Expression right = bin.getRight();
			int op = bin.getOp();

			if (!(op == ExprBinary.BINOP_AND || op == ExprBinary.BINOP_OR)) {
				Expression origin = bin;
				MakeProtections leftPrtcs = new MakeProtections();
				left.accept(leftPrtcs);
				for (Expression ei : leftPrtcs.getTmp()) {
					origin = new ExprBinary(bin, ei, "&&", origin);
				}
				MakeProtections rightPrtcs = new MakeProtections();
				right.accept(rightPrtcs);
				for (Expression ei : rightPrtcs.getTmp()) {
					origin = new ExprBinary(bin, ei, "&&", origin);
				}
				return origin;
			} else {
				left = (Expression) left.accept(this);
				right = (Expression) right.accept(this);
				return new ExprBinary(bin, op, left, right);
			}
		}
	}

	class MakeProtections extends FEReplacer {
		List<Expression> tmp;

		public MakeProtections() {
			tmp = new LinkedList<Expression>();
		}

		public Object visitExprArrayRange(ExprArrayRange arr) {

			Expression idx = makeLocalIndex(arr);
			Expression base = (Expression) arr.getBase().accept(this);
			Expression cond = null;

			if (arr.hasSingleIndex()) {
				cond = makeGuard(base, idx);
			} else {
				RangeLen rl = arr.getSelection();
				Expression ofst = (Expression) rl.getLenExpression().accept(this);
				cond = makeGuard(base, ofst, idx);
			}

			if (cond == null || arr.getCx().getAut()) {
				return super.visitExprArrayRange(arr);
			}
			tmp.add(cond);
			return arr;
		}

		public Object visitExprBinary(ExprBinary bin) {
			Expression right = bin.getRight();
			int op = bin.getOp();
			if (op == ExprBinary.BINOP_DIV || op == ExprBinary.BINOP_MOD) {
				tmp.add(new ExprBinary(bin, ExprBinary.BINOP_NEQ, right, new ExprConstInt(0)));
			}
			return bin;
		}

		public List<Expression> getTmp() {
			return tmp;
		}


	protected Expression makeLocalIndex(ExprArrayRange ear) {
		RangeLen rl = ear.getSelection();
		Expression nofset = (Expression) rl.start();
		return nofset;
	}

	protected Expression makeGuard(Expression base, Expression idx) {
		Expression sz = ((TypeArray) getType(base)).getLength();
		return new ExprBinary(new ExprBinary(idx, ">=", ExprConstInt.zero), "&&", new ExprBinary(idx, "<", sz));
	}

	protected Expression makeGuard(Expression base, Expression len, Expression idx) {
		Expression sz = ((TypeArray) getType(base)).getLength();
		Expression ex = new ExprBinary(idx, ">=", ExprConstInt.zero);
		Expression eb = new ExprBinary(len, ">=", ExprConstInt.zero);
		Expression e2 = new ExprBinary(new ExprBinary(idx, "+", len), "<=", sz);

		if (sz == null) {
			return ex;
		}
		return new ExprBinary(ex, "&&", new ExprBinary(eb, "&&", e2));
	}

}

}
