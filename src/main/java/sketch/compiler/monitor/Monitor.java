package sketch.compiler.monitor;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Map;

import sketch.compiler.ast.core.exprs.Expression;
import sketch.util.Pair;

public class Monitor {

	private int[] states;
	private int initState;
	private Map<Pair<Expression, Integer>, Integer> delta;
	private int[] finalStates;
	
	public Monitor(int[] states, int initState, Map<Pair<Expression, Integer>, Integer> delta, int[] finalStates) {
		this.setStates(states);
		this.setInitState(initState);
		this.setDelta(delta);
		this.setFinalStates(finalStates);
	}

	public int[] getStates() {
		return states;
	}

	public void setStates(int[] states) {
		this.states = states;
	}

	public int getInitState() {
		return initState;
	}

	public void setInitState(int initState) {
		this.initState = initState;
	}

	public Map<Pair<Expression, Integer>, Integer> getDelta() {
		return delta;
	}

	public void setDelta(Map<Pair<Expression, Integer>, Integer> delta) {
		this.delta = delta;
	}

	public int[] getFinalStates() {
		return finalStates;
	}

	public void setFinalStates(int[] finalStates) {
		this.finalStates = finalStates;
	}

	public boolean isInit(int state) {
		return this.getInitState() == state;
	}

	public LinkedList<Pair<Expression, Integer>> reverseDelta(int state) {
		LinkedList<Pair<Expression, Integer>> previous = new LinkedList<Pair<Expression, Integer>>();
		
		for (Map.Entry<Pair<Expression, Integer>, Integer> entry : this.getDelta().entrySet()) {
			if (entry.getValue() == Integer.valueOf(state)) {
				previous.add(entry.getKey());
			}
		}
		
		return previous;
	}

	public String toString() {
		String pambo = "";
		pambo += "--------------Monitor-------------\n";
		pambo += "--------------States--------------\n";
		pambo += Arrays.toString(this.getStates());
		pambo += "\n--------------Init State----------\n";
		pambo += Integer.toString(this.getInitState());
		pambo += "\n--------------Delta---------------\n";
		for (Pair<Expression, Integer> di : this.getDelta().keySet()) {
			pambo += di.toString() + "->" + (this.getDelta().get(di)).toString() + "\n";
		}
		pambo += "\n--------------Final States--------\n";
		pambo += Arrays.toString(this.getFinalStates());
		return pambo;
	}

}
