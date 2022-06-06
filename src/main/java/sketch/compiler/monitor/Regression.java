package sketch.compiler.monitor;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import sketch.compiler.ast.core.exprs.ExprBinary;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.util.Pair;

public class Regression {

	private LinkedList<Expression[]> table;
	private Monitor m;

	public Regression(Monitor m) {
		this.setM(m);
		this.setTable(new LinkedList<Expression[]>());
		int numStates = m.getStates().length;
		Expression[] timeZero = new Expression[numStates];
		for (int i = 0; i < timeZero.length; ++i) {
			timeZero[i] = m.isInit(i) ? (Expression) new ExprConstInt(1) : (Expression) new ExprConstInt(0);
		}
		table.add(timeZero);
	}

	public LinkedList<Expression[]> getTable() {
		return table;
	}

	public void setTable(LinkedList<Expression[]> table) {
		this.table = table;
	}
	
	public Monitor getM() {
		return m;
	}

	public void setM(Monitor m) {
		this.m = m;
	}

	public String toString() {
		String pambo = "";
		pambo += m.toString();
		pambo += "\n----------------------------------\n";
		pambo += "----------------Table-------------\n";
		for (int i = 0; i < this.getTable().size(); i++) {
			pambo += Arrays.toString(this.getTable().get(i)) + "\n";
		}
		return pambo;
	}

	/*
	 * We're assuming, for the moment, the following grammar for statements. c
	 * ::= x := e | c;c | skip
	 */
	public void updateTable(List<Statement> block) {
		LinkedList<Expression[]> actual = this.getTable();
		Expression[] lastTimestamp = new Expression[m.getStates().length];
		for (Statement ci : block) {
			lastTimestamp = actual.getLast();
			Expression[] newTimestamp = new Expression[m.getStates().length];
			for (int j = 0; j < lastTimestamp.length; j++) {
				LinkedList<Pair<Expression, Integer>> qe = m.reverseDelta(j);
				newTimestamp[j] = constructNE(qe, lastTimestamp);
			}
			actual.addLast(newTimestamp);
		}
	}

	private Expression constructNE(LinkedList<Pair<Expression, Integer>> qe, Expression[] lastTimestamp) {
		LinkedList<Expression> conjucts = new LinkedList<Expression>();
		for (Pair<Expression, Integer> qei : qe) {
			Expression eLeft = lastTimestamp[qei.getSecond()];
			if (eLeft.equals((Expression) new ExprConstInt(1))) {
				conjucts.add(qei.getFirst());
			} else if (eLeft.equals((Expression) new ExprConstInt(0))) {
				conjucts.add((Expression) new ExprConstInt(0));
			} else {
				conjucts.add((Expression) new ExprBinary(6, eLeft, qei.getFirst()));
			}
		}
		Expression eNew = conjucts.getFirst();
		conjucts.removeFirst();
		for (Expression eOld : conjucts) {
			eNew = eNew.equals(new ExprConstInt(0)) ? eOld : (Expression) new ExprBinary(7, eNew, eOld);
		}
		return eNew;
	}

}
