package streamit.frontend.tosbit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import streamit.frontend.nodes.ExprArray;
import streamit.frontend.nodes.ExprArrayInit;
import streamit.frontend.nodes.ExprBinary;
import streamit.frontend.nodes.ExprConstInt;
import streamit.frontend.nodes.ExprFunCall;
import streamit.frontend.nodes.ExprStar;
import streamit.frontend.nodes.ExprUnary;
import streamit.frontend.nodes.ExprVar;
import streamit.frontend.nodes.Expression;
import streamit.frontend.nodes.FEContext;
import streamit.frontend.nodes.FENode;
import streamit.frontend.nodes.FEReplacer;
import streamit.frontend.nodes.FieldDecl;
import streamit.frontend.nodes.Function;
import streamit.frontend.nodes.Parameter;
import streamit.frontend.nodes.Statement;
import streamit.frontend.nodes.StmtAssign;
import streamit.frontend.nodes.StmtBlock;
import streamit.frontend.nodes.StmtFor;
import streamit.frontend.nodes.StmtLoop;
import streamit.frontend.nodes.StmtVarDecl;
import streamit.frontend.nodes.StreamSpec;
import streamit.frontend.nodes.StreamType;
import streamit.frontend.nodes.TempVarGen;
import streamit.frontend.nodes.Type;
import streamit.frontend.nodes.TypeArray;
import streamit.frontend.nodes.TypePrimitive;

class FindIndetNodes extends FEReplacer{
	public final Map<FENode, String> nodes;
	private final StreamSpec curSpec;
	private Set<Function> visitedFunctions;
	public FindIndetNodes(StreamSpec curSpec){
		nodes = new HashMap<FENode, String>();
		this.curSpec = curSpec;
		visitedFunctions = new HashSet<Function>();
	}
	public Object visitExprStar(ExprStar star) {
		nodes.put(star, "_oracle_" + nodes.size());
		return null;
	}
	public Object visitExprBinary(ExprBinary exp)
    {
		if(exp.getOp()==ExprBinary.BINOP_SELECT){
			nodes.put(exp, "_oracle_" + nodes.size());
		}
		return super.visitExprBinary(exp);
    }
	public Object visitExprFunCall(ExprFunCall exp)
    {	
		Function fun = curSpec.getFuncNamed(exp.getName());
		assert fun != null : "Calling undefined function!!";
		Object obj = super.visitExprFunCall(exp);
		if(!visitedFunctions.contains(fun)){
			visitedFunctions.add(fun);
			fun.accept(this);			
		}
		return obj;
    }
}
