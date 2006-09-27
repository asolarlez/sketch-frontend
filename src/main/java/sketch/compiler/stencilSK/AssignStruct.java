package streamit.frontend.stencilSK;

import java.util.ArrayList;
import java.util.List;

import streamit.frontend.nodes.ExprArrayRange;
import streamit.frontend.nodes.ExprVar;
import streamit.frontend.nodes.Expression;



class AssignStruct{
	Expression rhs;
	String lhsName;
	List<Expression> indices;
	List<AssignStruct> predecessors;
	
	AssignStruct(Expression lhs, Expression rhs){
		this.rhs = rhs;
		if( lhs instanceof ExprVar ){
			lhsName = ((ExprVar)lhs).getName();
			indices = new ArrayList<Expression>();
			predecessors = new ArrayList<AssignStruct>();
		}else if( lhs instanceof ExprArrayRange ){
			ExprArrayRange ear = (ExprArrayRange) lhs;
			lhsName = ear.getAbsoluteBase().getName();
			indices = ear.getArrayIndices();
			predecessors = new ArrayList<AssignStruct>();
		}else
			assert false;
	}
	
	
}