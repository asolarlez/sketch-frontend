package streamit.frontend.passes;

import java.util.ArrayList;
import java.util.List;

import streamit.frontend.nodes.ExprArrayInit;
import streamit.frontend.nodes.ExprArrayRange;
import streamit.frontend.nodes.ExprConstInt;
import streamit.frontend.nodes.ExprConstant;
import streamit.frontend.nodes.ExprNullPtr;
import streamit.frontend.nodes.Expression;
import streamit.frontend.nodes.FEReplacer;
import streamit.frontend.nodes.ExprArrayRange.Range;
import streamit.frontend.nodes.ExprArrayRange.RangeLen;

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
