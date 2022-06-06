package sketch.compiler.monitor;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.exprs.ExprBinary;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprUnary;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.util.Pair;

public class MainTestMonitor {

	public static void main(String[] args) {
		
		int[] states = new int[] { 0, 1, 2 };
		int initState = 0;
		FENode context = null;
		
		Expression varX = (Expression) new ExprVar(context, "x");
		Expression e1 = (Expression) new ExprUnary("!",
				(Expression) new ExprBinary(8, varX, (Expression) new ExprConstInt(5)));
		Expression e2 = (Expression) new ExprBinary(8, varX, (Expression) new ExprConstInt(5));
		Expression e3 = (Expression) new ExprUnary("!",
				(Expression) new ExprBinary(8, varX, (Expression) new ExprConstInt(4)));
		Expression e4 = (Expression) new ExprBinary(8, varX, (Expression) new ExprConstInt(4));

		Map<Pair<Expression, Integer>, Integer> delta = new HashMap<Pair<Expression, Integer>, Integer>();

		delta.put(new Pair<Expression, Integer>(e1, Integer.valueOf(0)), Integer.valueOf(0));
		delta.put(new Pair<Expression, Integer>(e2, Integer.valueOf(0)), Integer.valueOf(1));
		delta.put(new Pair<Expression, Integer>(e3, Integer.valueOf(1)), Integer.valueOf(1));
		delta.put(new Pair<Expression, Integer>(e4, Integer.valueOf(1)), Integer.valueOf(2));

		int[] finalStates = new int[] { 2 };

		Monitor m = new Monitor(states, initState, delta, finalStates);

		Regression reg = new Regression(m);

		Statement c1 = (Statement) new StmtAssign(varX, (Expression) new ExprConstInt(1));
		Statement c2 = (Statement) new StmtAssign(varX, (Expression) new ExprConstInt(2));
		Statement c3 = (Statement) new StmtAssign(varX, (Expression) new ExprConstInt(3));

		List<Statement> program = new LinkedList<Statement>();
		program.add(c1);
		program.add(c2);
		program.add(c3);

		reg.updateTable(program);

		System.out.println(reg.toString());

	}

}
