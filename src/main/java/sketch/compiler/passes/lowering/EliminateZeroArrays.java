package sketch.compiler.passes.lowering;
import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.exprs.ExprArrayInit;
import sketch.compiler.ast.core.exprs.ExprArrayRange;
import sketch.compiler.ast.core.exprs.Expression;

public class EliminateZeroArrays extends FEReplacer {
	
	public Object visitExprArrayRange(ExprArrayRange exp) {		
		final Expression newBase=exp.getBase();
		if(newBase instanceof ExprArrayInit){
			ExprArrayInit eai = (ExprArrayInit)newBase;
			Expression rv = null;
			for(Expression elem : eai.getElements()){				
					if(rv != null && !elem.equals(rv)){
						return super.visitExprArrayRange(exp);
					}
					rv = elem;
			}
			return rv;
		}
		return super.visitExprArrayRange(exp);
	}

}
