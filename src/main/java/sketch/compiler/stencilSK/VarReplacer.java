package streamit.frontend.stencilSK;

import streamit.frontend.nodes.ExprVar;
import streamit.frontend.nodes.Expression;
import streamit.frontend.nodes.FEReplacer;



class VarReplacer extends FEReplacer{
	String oldName;
	Expression newName;
	
	VarReplacer(String oldName, Expression newName){
		this.oldName = oldName;
		this.newName = newName;
	}
	
	public Object visitExprVar(ExprVar exp) {
		if( exp.getName().equals(oldName)){
			return newName;
		}else{
			return exp;
		}
	}
	
}

