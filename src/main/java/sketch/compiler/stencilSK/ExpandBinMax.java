package streamit.frontend.stencilSK;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import streamit.frontend.nodes.ExprArray;
import streamit.frontend.nodes.ExprBinary;
import streamit.frontend.nodes.ExprConstInt;
import streamit.frontend.nodes.ExprFunCall;
import streamit.frontend.nodes.ExprVar;
import streamit.frontend.nodes.Expression;
import streamit.frontend.nodes.Statement;
import streamit.frontend.nodes.StmtAssign;
import streamit.frontend.nodes.StmtBlock;
import streamit.frontend.nodes.StmtIfThen;
import streamit.frontend.nodes.StmtVarDecl;
import streamit.frontend.nodes.TypeArray;
import streamit.frontend.nodes.TypePrimitive;

public class ExpandBinMax {	

	public ExpandBinMax() {
		super();
		// TODO Auto-generated constructor stub
	}

	
	
	public Expression comp(int pos, int dim, ExprVar v1, ExprVar v2){
		ExprArray ear1 = new ExprArray(null, v1, new ExprConstInt(pos));
		ExprArray ear2 = new ExprArray(null, v2, new ExprConstInt(pos));
		Expression tmp = new ExprBinary(null,ExprBinary.BINOP_LT, ear1, ear2);
		Expression eq =  new ExprBinary(null,ExprBinary.BINOP_EQ, ear1, ear2);
		Expression out;
		if(pos<dim-1){
			Expression andExp = new ExprBinary(null, ExprBinary.BINOP_AND, eq, comp(pos+1, dim,  v1, v2));
			out = new ExprBinary(null, ExprBinary.BINOP_OR, tmp, andExp);
		// out = tmp || (eq &&  comp(iterIt))
		}else{
			out = tmp;
		// out = tmp;
		}
		return out;		
	}
	
	
	public Statement processMax(int dim, ExprVar v1, ExprVar v2, String gv2, int id){
		Expression cond1 = new ExprVar(null, gv2);
		Expression cond2 = comp(0, dim, v1, v2);
		
		StmtAssign as1 = new StmtAssign(null, new ExprVar(null, ArrFunction.IND_VAR), new ExprConstInt(id+1));
		StmtAssign as2 = new StmtAssign(null, v1, v2);
		List<Statement> lst = new ArrayList<Statement>(2);
		lst.add(as1);
		lst.add(as2);
		
		StmtIfThen if2 = new StmtIfThen(null, cond2,  new StmtBlock(null, lst), null);
		StmtIfThen if1 = new StmtIfThen(null, cond1,  if2, null);		
		return if1;
	}
	

	
	
}
