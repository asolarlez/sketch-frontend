package sketch.compiler.passes.lowering;
import java.util.ArrayList;
import java.util.List;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.exprs.ExprArray;
import sketch.compiler.ast.core.exprs.ExprArrayRange;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.exprs.ExprArrayRange.RangeLen;

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
