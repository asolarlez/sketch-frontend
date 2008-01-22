package streamit.frontend.experimental.eliminateTransAssign;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import streamit.frontend.experimental.DataflowWithFixpoint;
import streamit.frontend.experimental.abstractValue;
import streamit.frontend.nodes.ExprField;
import streamit.frontend.nodes.ExprVar;
import streamit.frontend.nodes.Expression;
import streamit.frontend.nodes.Function;
import streamit.frontend.nodes.StmtAssign;
import streamit.frontend.nodes.StreamSpec;
import streamit.frontend.tosbit.recursionCtrl.BaseRControl;

public class EliminateTransitiveAssignments extends DataflowWithFixpoint {

	protected List<Function> functionsToAnalyze(StreamSpec spec){
	    return new LinkedList<Function>(spec.getFuncs());
    }
	
	public EliminateTransitiveAssignments(){
		super(TAvalueType.vtype, null, true, 0, (new BaseRControl(10)));
	}
	
	public Object visitExprVar(ExprVar exp) {		
		String vname =  exp.getName();
		transAssignAbsValue val = (transAssignAbsValue)state.varValue(vname);
		transAssignAbsValue oval = val;
		String nm = vname;
		while(val.varIamEqualTo != null){
			String oldNm = nm;
			nm = state.untransName(val.varIamEqualTo);
			if(oldNm.equals(nm)){
				break;
			}
			val = (transAssignAbsValue)state.varValue(nm);
		}
		if(isReplacer){
			exprRV = new ExprVar(exp.getCx(), transName(nm));
		}
		return 	oval;
	}
	
	/*
    protected Object assignmentToField(String lhsName, StmtAssign stmt, abstractValue rhs, Expression nlhs, Expression nrhs){
    	ExprField ef = (ExprField) nlhs; 
    	while(ef.getLeft() instanceof ExprField){
    		
    	}
    	return isReplacer?  new StmtAssign(stmt.getCx(), nlhs, nrhs, stmt.getOp())  : stmt;
    }
    */
	
}
