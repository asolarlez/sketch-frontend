package sketch.compiler.smt.partialeval;

import java.util.HashMap;
import java.util.Set;

import sketch.compiler.ast.core.typs.Type;

public class LinearNode extends NodeToSmtValue {
	
	protected int mC;
	
	
	protected LinearNode(Type t, int numBits) {
		super(null, t, SmtStatus.BOTTOM, numBits,
				new HashMap<VarNode, Integer>());
		this.hashCode = computeHash();
	}
	
	/**
	 * Create a LinearNode object equivalent to a single
	 * VarNode
	 * @param v
	 * @param t
	 * @param numBits
	 */
	public LinearNode(VarNode v) {
	    this(v.getType(), v.getNumBits());
	    increment(v, 1);
	}
	
	/**
	 * Create a LinearNode obj equivalent to a constant
	 * @param c
	 */
	public LinearNode(ConstNode c) {
	    this(c.getType(), c.getNumBits());
        increment(null, c.getIntVal());
	}
	
	/**
	 * Create a LinearNode obj by adding n1 and n2
	 * @param n1
	 * @param n2
	 */
	public LinearNode(LinearNode n1, LinearNode n2, boolean isMinus) {
		this(n1.getType(), Math.max(n1.getNumBits(), n2.getNumBits()));
		
		for (VarNode v : n1.getVars()) {
			increment(v, n1.getCoeff(v));
		}
		
		if (isMinus) {
		    for (VarNode v : n2.getVars()) {
                decrement(v, n2.getCoeff(v));
            }
		} else {
        	for (VarNode v : n2.getVars()) {
        		increment(v, n2.getCoeff(v));
        	}
		}
		
		if (isMinus)
		    mC = n1.getCoeff(null) - n2.getCoeff(null);
		else
		    mC = n1.getCoeff(null) + n2.getCoeff(null);
	}
	
	/**
	 * Create a LinearNode obj by multiplying n1 with c
	 * @param n1
	 * @param c
	 */
	public LinearNode(LinearNode n1, ConstNode c) {
		this(n1.getType(), Math.max(n1.getNumBits(), c.getNumBits()));
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
	
	public int getNumTerms() {
	    return getVars().size();
	}
	
	public void increment(VarNode var, int coeffToAdd) {
		int coeff = -1;
		if (var == null) {
		    mC += coeffToAdd;
		    return;
		}
		
		if (getVarToCoeffMap().containsKey(var)) {
			// case 1
			// this has var, add coeff
			coeff = getVarToCoeffMap().get(var);
			coeff += coeffToAdd;
			if (coeff == 0)
			    getVarToCoeffMap().remove(var);
			else
			    getVarToCoeffMap().put(var, coeff);	
			
		} else {
			// case 2: this has NO var
			//			add var to one of the var to track
			//			addd coeff
			getVarToCoeffMap().put(var, coeffToAdd);
		}
	}
	
	public void decrement(VarNode var, int coeffToAdd) {
	    increment(var, -coeffToAdd);
	}
	
	public int getCoeff(VarNode var) {
		if (var == null)
			return mC;
		
		if (getVarToCoeffMap().containsKey(var))
			return getVarToCoeffMap().get(var);
		else
			return 0;
	}
	
	@Override
	public String toString() {
	    StringBuffer sb = new StringBuffer();
	    sb.append(mC);
	    
	    for (VarNode v : getVars()) {
	        sb.append("+");
	       sb.append(getCoeff(v));
	       sb.append('*');
	       sb.append(v);
	    }
	    return sb.toString();
	}
	
	@Override
	public boolean equals(Object obj) {
	    if (this == obj) return true;
	    if (obj instanceof LinearNode) {
            LinearNode that = (LinearNode) obj;
            
            if (this.mC != that.mC)
                return false;
            
            if (this.getVars().size() != that.getVars().size())
                return false;
            
            for (VarNode t : this.getVars()) {
                if (!that.getVars().contains(t))
                    return false;
                if (this.getCoeff(t) != that.getCoeff(t))
                    return false;
            }
            return true;  
        }
        return false;
	}
	
	@Override
	public int hashCode() {
	    return hashCode;
	}
	
	public int computeHash() {
	    return this.mC ^ obj.hashCode();
	}

    @Override
    public Object accept(FormulaVisitor fv) {
        return fv.visitLinearNode(this);
    }

	
}
