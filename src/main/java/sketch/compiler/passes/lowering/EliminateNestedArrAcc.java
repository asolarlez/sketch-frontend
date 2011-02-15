package sketch.compiler.passes.lowering;
import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.exprs.ExprArrayRange;
import sketch.compiler.ast.core.exprs.ExprArrayRange.RangeLen;
import sketch.compiler.ast.core.exprs.ExprBinary;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.StmtAssert;

public class EliminateNestedArrAcc extends FEReplacer {

    boolean bcheck;
    public EliminateNestedArrAcc(boolean bcheck){
        this.bcheck = bcheck;
    }
	public Object visitExprArrayRange(ExprArrayRange exp) {
		
		RangeLen rl = exp.getSelection();
		Expression newStart = doExpression( rl.start());
		Expression newBase = doExpression(exp.getBase());
		Expression newLen = doExpression(rl.getLenExpression());
		if(newBase  instanceof ExprArrayRange){
			ExprArrayRange baserange = (ExprArrayRange) newBase;
			RangeLen baserl = baserange.getSelection();			
			Expression nstart = new ExprBinary(exp, ExprBinary.BINOP_ADD, baserl.start(), newStart  );
			if(baserl.hasLen()){
    			if(bcheck){
    			    Expression cond;
    			    if(newLen == null){
    			        cond = new ExprBinary(newStart, "<=", baserl.getLenExpression());
    			    }else{
    			        cond = new ExprBinary(new ExprBinary(newStart, "+",newLen), "<=", baserl.getLenExpression());
    			    }			    
    			    addStatement(new StmtAssert(cond, exp.getCx() + ": Array out of bounds", false));
    			    return new ExprArrayRange(exp, baserange.getBase(), new RangeLen(nstart, rl.getLenExpression()), exp.isUnchecked());
    			}else{
    			    throw new RuntimeException("NYI");
    			    //int nlen = rl.len() > baserl.len() ? baserl.len() : rl.len();
    			    //return new ExprArrayRange(exp, baserange.getBase(), new RangeLen(nstart, nlen), exp.isUnchecked());
    			}
			}
		}
		return new ExprArrayRange(exp, newBase, new RangeLen(newStart, newLen), exp.isUnchecked());
	}


}
