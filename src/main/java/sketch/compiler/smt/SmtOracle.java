package sketch.compiler.smt;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.smt.partialeval.NodeToSmtValue;
import sketch.compiler.smt.partialeval.NodeToSmtVtype;
import sketch.compiler.smt.partialeval.SmtType;
import sketch.compiler.smt.partialeval.VarNode;
import sketch.compiler.solvers.constructs.AbstractValueOracle;

public abstract class SmtOracle extends AbstractValueOracle {
	
	/**
	 * After sketch is resolved, this map will contain the value of each
	 * variable in store.
	 */
	protected SortedMap<String, NodeToSmtValue> valMap;
	
	/**
	 * The formula to which this mOutputOracle is a solution
	 */
	protected NodeToSmtVtype mFormula;
	
	/**
	 * The vtype object that will be using this mOutputOracle as input
	 */
	protected NodeToSmtVtype mVtype;
	
	/**
	 * A map that maps from array name to maps of the idx and value.
	 * String -> (int -> int)
	 */
	protected Map<String, Map<Integer, NodeToSmtValue>> arrayValueMap;

	/**
	 * The number of bits used to represent an integer
	 */
	protected int mIntNumBits;
	
	protected HashSet<String> mVarsOfInterest;
	
	/*
	 * Getters & Setters
	 */
	public void setVtype(NodeToSmtVtype vtype) {
		mVtype = vtype;
	}
	
	/**
	 * Constructor
	 * @param holeTracker
	 */
	public SmtOracle() {
		super(null);
		
		arrayValueMap = new HashMap<String, Map<Integer,NodeToSmtValue>>();
		valMap = new TreeMap<String, NodeToSmtValue>();
		mVarsOfInterest = new HashSet<String>();
	}
	
	public Map<String, NodeToSmtValue> getValueMap() {
		return valMap;
	}
	
	public Map<String, Map<Integer, NodeToSmtValue>> getArrayValueMap() {
		return arrayValueMap;
	}
	
	public NodeToSmtValue getValueForArray(String arrayName, int idx) {
		return arrayValueMap.get(arrayName).get(idx);
	}

	
	/**
	 * This method exists for testing purpose only
	 * 
	 * @param var the name of the variables as they appear in smt input formulas
	 * @param smtType TODO
	 * @return the 
	 */
	public NodeToSmtValue getValueForVariable(String var, SmtType smtType) {
		if (valMap.containsKey(var))
			return this.valMap.get(var);
		else
			return NodeToSmtVtype.defaultValue(smtType);
	}
	
	public void putValueForVariable(String var, NodeToSmtValue value) {
		this.valMap.put(var, value);
	}
	
	public NodeToSmtValue getValueForVariable(VarNode lhs) {
		String varName = lhs.getRHSName();
		return getValueForVariable(varName, lhs.getSmtType());
	}
	
	
	@Override
	public Expression popValueForNode(FENode node) {
		String name = holeNamer.getName(node);
		NodeToSmtValue v = valMap.get(name);
		
		return nodeToExpression(node, v); 
	}

	public void linkToFormula(NodeToSmtVtype formula) {
		mFormula = formula;
		holeNamer = formula.getHoleNamer();
	}
	
	public Set<String> keySet() {
		return valMap.keySet();
	}
	
	public int size() {
		return valMap.size();
	}
	
	public abstract Expression nodeToExpression(FENode context, NodeToSmtValue value);
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("Array:\t");
		sb.append(arrayValueMap.toString());
		sb.append('\n');
		sb.append("Vars:\t");
		sb.append(valMap.toString());
		
		return sb.toString();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof SmtOracle) {
			SmtOracle oracle = (SmtOracle) obj;
			return valMap.equals(oracle.valMap);
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return valMap.hashCode();
	}
}
