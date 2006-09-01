package streamit.frontend.stencilSK;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import streamit.frontend.nodes.Expression;
import streamit.frontend.nodes.FEVisitor;
import streamit.frontend.nodes.Statement;


/**
 * Represents a max statement of the form:
 * int[dim] lhsvar = max{ primPred(lhsvar) && secPred(lhsvar) };
 */
public class StmtMax extends Statement{
	int dim;
	String lhsvar;	
	String indvar;
	/**
	 * Primary constraints.
	 * They are of the form expr(idx)==t
	 */
	List<Expression> primC;
	/**
	 * Secondary constraints.
	 * They are of the form idx < something
	 */
	List<Expression> secC;
	/**
	 * Tertiary constraints.
	 * They are pretty arbitrary.
	 */
	List<Expression> terC;
	public Object accept(FEVisitor visitor){
		assert false;
		return null;
	}
	StmtMax(int dim, String lhsvar, String indvar){super(null);
		this.indvar = indvar;
		this.dim = dim;
		this.lhsvar = lhsvar;
		this.primC = new ArrayList<Expression>();
		this.secC = new ArrayList<Expression>();
		this.terC = new ArrayList<Expression>();
	}
	String getVar(){ assert false; return null;}
	public String toString(){
		String rv = "int[" + dim + "] " + lhsvar + "= max{";
		for(Iterator<Expression> eit = primC.iterator(); eit.hasNext(); ){
			rv += eit.next().toString() + " & ";
		}
		for(Iterator<Expression> eit = secC.iterator(); eit.hasNext(); ){
			rv += eit.next().toString() + " & ";
		}
		for(Iterator<Expression> eit = terC.iterator(); eit.hasNext(); ){
			rv += eit.next().toString() + " & ";
		}
		rv += "}; \n";
		rv += "bit " + indvar + " = " + lhsvar + " != null";
		return rv;
	}
}