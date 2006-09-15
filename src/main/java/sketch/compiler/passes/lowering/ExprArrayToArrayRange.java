package streamit.frontend.passes;

import java.util.ArrayList;
import java.util.List;

import streamit.frontend.nodes.ExprArray;
import streamit.frontend.nodes.ExprArrayRange;
import streamit.frontend.nodes.Expression;
import streamit.frontend.nodes.FEReplacer;
import streamit.frontend.nodes.ExprArrayRange.RangeLen;

public class ExprArrayToArrayRange extends FEReplacer {
	public Object visitExprArray(ExprArray exp)
    {
        Expression base = doExpression(exp.getBase());
        Expression offset = doExpression(exp.getOffset());
        List lst = new ArrayList();
		lst.add( new RangeLen(offset, 1) );
		return new ExprArrayRange(base, lst);		        
    }
}
