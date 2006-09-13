package streamit.frontend.stencilSK;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import streamit.frontend.nodes.ExprArray;
import streamit.frontend.nodes.ExprBinary;
import streamit.frontend.nodes.ExprConstInt;
import streamit.frontend.nodes.ExprVar;
import streamit.frontend.nodes.Expression;
import streamit.frontend.nodes.FEVisitor;
import streamit.frontend.nodes.Statement;
import streamit.frontend.nodes.StmtAssign;
import streamit.frontend.nodes.StmtBlock;
import streamit.frontend.nodes.StmtVarDecl;
import streamit.frontend.nodes.TypeArray;
import streamit.frontend.nodes.TypePrimitive;


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
	
	
	/**
	 * If the StmtMax was successfully converted to a set of assignments, 
	 * these will be stored in the maxAssign field.
	 */
	Statement maxAssign = null;
	
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
		if( maxAssign != null )
			return maxAssign.toString();
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
	
	
	public boolean resolve(){
		ResolveMax rmax = new ResolveMax(this);
		rmax.run();
		for(int i=0; i<dim; ++i){
			if( rmax.expArr[i] == null )
				return false;
		}
		List<Statement> statements = new ArrayList<Statement>();		
		ExprVar base = new ExprVar(null, this.lhsvar);
		
		statements.add(  new StmtVarDecl(null, new TypeArray( TypePrimitive.inttype, new ExprConstInt(dim) ), this.lhsvar, null) );
		statements.add(  new StmtVarDecl(null, TypePrimitive.bittype, this.indvar, null) );
		for(int i=0; i<dim; ++i){
			ExprArray ea = new ExprArray(null, base, new ExprConstInt(i));
			StmtAssign ass = new StmtAssign(null, ea, rmax.expArr[i]);
			statements.add(ass);
		}
		ExprVar indicator  = new ExprVar(null, this.indvar);
		Expression cond = null;
		for(Iterator<Expression> eit = primC.iterator(); eit.hasNext(); ){
			if( cond == null)
				cond = eit.next();
			else
				cond = new ExprBinary(null, ExprBinary.BINOP_AND, cond, eit.next());
		}
		for(Iterator<Expression> eit = secC.iterator(); eit.hasNext(); ){
			if( cond == null)
				cond = eit.next();
			else
				cond = new ExprBinary(null, ExprBinary.BINOP_AND, cond, eit.next());
		}
		for(Iterator<Expression> eit = terC.iterator(); eit.hasNext(); ){
			if( cond == null)
				cond = eit.next();
			else
				cond = new ExprBinary(null, ExprBinary.BINOP_AND, cond, eit.next());
		}
		for(Iterator<Expression> eit = rmax.moreConstraints.iterator(); eit.hasNext(); ){
			if( cond == null)
				cond = eit.next();
			else
				cond = new ExprBinary(null, ExprBinary.BINOP_AND, cond, eit.next());
		}
		StmtAssign ass = new StmtAssign(null, indicator,cond);
		statements.add(ass);
		maxAssign = new StmtBlock(null, statements);
		return true;
	}
	
	
}