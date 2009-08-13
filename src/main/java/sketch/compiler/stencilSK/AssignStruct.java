package sketch.compiler.stencilSK;

import java.util.ArrayList;
import java.util.List;

import sketch.compiler.ast.core.exprs.ExprArrayRange;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;



class AssignStruct{
	Expression rhs;
	String lhsName;
	List<Expression> indices;
	List<AssignStruct> predecessors;
	List<AbstractArray> inputs = new ArrayList<AbstractArray>();
	List<List<Expression>> inputIndices = new ArrayList<List<Expression>>();
	
	
	public void addInput(AbstractArray aa, List<Expression> el){
		inputs.add(aa);
		inputIndices.add(el);
	}
	
	public AssignStruct(Expression lhs, Expression rhs){
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