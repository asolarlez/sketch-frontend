package sketch.compiler.passes.lowering;
import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.exprs.ExprArrayRange;
import sketch.compiler.ast.core.exprs.ExprBinary;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.exprs.ExprArrayRange.RangeLen;

public class EliminateNestedArrAcc extends FEReplacer {



	public Object visitExprArrayRange(ExprArrayRange exp) {
		assert exp.getMembers().size() == 1 && exp.getMembers().get(0) instanceof RangeLen : "Complex indexing not yet implemented.";
		RangeLen rl = (RangeLen)exp.getMembers().get(0);
		Expression newStart = (Expression) rl.start().accept(this);
		Expression newBase = (Expression) exp.getBase().accept(this);
		if(newBase  instanceof ExprArrayRange){
			ExprArrayRange baserange = (ExprArrayRange) newBase;
			RangeLen baserl = (RangeLen)baserange.getMembers().get(0);
			int nlen = rl.len() > baserl.len() ? baserl.len() : rl.len();
			Expression nstart = new ExprBinary(exp, ExprBinary.BINOP_ADD, baserl.start(), newStart  );
			return new ExprArrayRange(exp, baserange.getBase(), new RangeLen(nstart, nlen), exp.isUnchecked());
		}
		return new ExprArrayRange(exp, newBase, new RangeLen(newStart, rl.len()), exp.isUnchecked());
	}


}
