package sketch.compiler.dataflow.eliminateTransAssign;

import java.util.LinkedList;
import java.util.List;

import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Package;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.dataflow.DataflowWithFixpoint;
import sketch.compiler.dataflow.abstractValue;
import sketch.compiler.dataflow.recursionCtrl.BaseRControl;

public class EliminateTransAssns extends DataflowWithFixpoint {

	protected List<Function> functionsToAnalyze(Package spec){
	    return new LinkedList<Function>(spec.getFuncs());
    }

    boolean inArrLen = false;

    public Object visitTypeArray(TypeArray t) {
        Type nbase = (Type) t.getBase().accept(this);
        inArrLen = true;
        try {
            abstractValue avlen = (abstractValue) t.getLength().accept(this);
        } finally {
            inArrLen = false;
        }
        Expression nlen = exprRV;
        if (nbase == t.getBase() && t.getLength() == nlen)
            return t;
        return isReplacer ? new TypeArray(nbase, nlen, t.getMaxlength()) : t;
    }


    public EliminateTransAssns(TempVarGen varGen) {
        super(TAvalueType.vtype, varGen, true, 0, (new BaseRControl(10)));
	}

	public Object visitExprVar(ExprVar exp) {
        if (inArrLen) {
            // This is too conservative; if you can replace exp with another final
            // variable, then it's ok to do it. The problem is when you try to
            // replace exp witn a non-final variable.
            return super.visitExprVar(exp);
        }
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
        return val;
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
