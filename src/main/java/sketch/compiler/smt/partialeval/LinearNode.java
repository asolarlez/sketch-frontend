package sketch.compiler.smt.partialeval;

import java.util.HashMap;
import java.util.Set;

import sketch.compiler.ast.core.typs.Type;

public class LinearNode extends NodeToSmtValue {
	
	protected int mC;
	
	public LinearNode(Type t, int numBits) {
		super(null, t, SmtStatus.BOTTOM, numBits,
				new HashMap<VarNode, Integer>());
	}
	
	public LinearNode(LinearNode n1, LinearNode n2) {
		this(n1.getType(), n1.getNumBits());
		
		for (VarNode v : n1.getVars()) {
			increment(v, n1.getCoeff(v));
		}
		
		for (VarNode v : n2.getVars()) {
			increment(v, n1.getCoeff(v));
		}
		
		mC = n1.getCoeff(null) + n2.getCoeff(null);
	}
	
	public LinearNode(LinearNode n1, ConstNode c) {
		this(n1.getType(), n1.getNumBits());
		obj = n1.getVarToCoeffMap().clone();
		mC = n1.mC;
		
		mC += c.getIntVal();
	}
	
	@SuppressWarnings("unchecked")
	protected HashMap<VarNode, Integer> getVarToCoeffMap() {
		return (HashMap<VarNode, Integer>) obj;
	}
	
	public Set<VarNode> getVars() {
		return getVarToCoeffMap().keySet();
	}
	
	public void increment(VarNode var, int coeffToAdd) {
		int coeff = -1;
		
		if (getVarToCoeffMap().containsKey(var)) {
			// case 1
			// this has var
			//			add coeff

			coeff = getVarToCoeffMap().get(var);
			coeff += coeffToAdd;
			getVarToCoeffMap().put(var, coeff);	
			
		} else {
			// case 2: this has NO var
			//			add var to one of the var to track
			//			addd coeff
			getVarToCoeffMap().put(var, coeffToAdd);
		}
	}
	
	public int getCoeff(VarNode var) {
		if (var == null)
			return mC;
		
		if (getVarToCoeffMap().containsKey(var))
			return getVarToCoeffMap().get(var);
		else
			return 0;
	}
	
	

	
}
