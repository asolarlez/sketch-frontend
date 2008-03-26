package streamit.frontend.experimental.eliminateTransAssign;

import java.util.LinkedList;
import java.util.List;

import streamit.frontend.experimental.DataflowWithFixpoint;
import streamit.frontend.nodes.ExprVar;
import streamit.frontend.nodes.Function;
import streamit.frontend.nodes.StreamSpec;
import streamit.frontend.tosbit.recursionCtrl.BaseRControl;

public class EliminateTransAssns extends DataflowWithFixpoint {

	protected List<Function> functionsToAnalyze(StreamSpec spec){
	    return new LinkedList<Function>(spec.getFuncs());
    }

	public EliminateTransAssns(){
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
			exprRV = new ExprVar(exp, transName(nm));
		}
		return 	oval;
	}

	/*
    protected Object assignmentToField(String lhsName, StmtAssign stmt, abstractValue rhs, Expression nlhs, Expression nrhs){
    	ExprField ef = (ExprField) nlhs;
    	while(ef.getLeft() instanceof ExprField){

    	}
    	return isReplacer?  new StmtAssign(stmt, nlhs, nrhs, stmt.getOp())  : stmt;
    }
    */

}
