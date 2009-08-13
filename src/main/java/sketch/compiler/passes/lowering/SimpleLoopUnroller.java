package sketch.compiler.passes.lowering;
import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.exprs.ExprBinary;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprUnary;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtExpr;
import sketch.compiler.ast.core.stmts.StmtFor;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.stencilSK.VarReplacer;

public class SimpleLoopUnroller extends FEReplacer {

	
	
	
	public static int decideForLoop(StmtFor stmt){
		if(!(stmt.getInit() instanceof StmtVarDecl)){
			return -1;
		}
		StmtVarDecl svd = (StmtVarDecl)stmt.getInit();
		if(svd.getNumVars() != 1){
			return -1;
		}
		Expression init = svd.getInit (0);
		stmt.assertTrue (null != init,
				"need an initializer for loop var '"+ svd.getName (0) +"'");
		Integer low = svd.getInit(0).getIValue();
		if(low == null){
			return -1;
		}
		String indName = svd.getName(0);
		if(!( stmt.getCond() instanceof ExprBinary )  ){
			return -1;
		}
		ExprBinary eb = (ExprBinary) stmt.getCond();
		if(eb.getOp() != ExprBinary.BINOP_LE && eb.getOp() != ExprBinary.BINOP_LT){
			return -1;
		}
		if( ! eb.getLeft().toString().equals(indName)  ){
			return -1;
		}
		Integer high = eb.getRight().getIValue();
		if(high == null){
			return -1;
		}
		int sz = high - low;
		if(  eb.getOp() == ExprBinary.BINOP_LE ){
			sz++;
		}

		if(stmt.getIncr() instanceof StmtAssign){
			StmtAssign sa = (StmtAssign)stmt.getIncr();
			if(!sa.getLHS().toString().equals(indName)){
				return -1;
			}

			if(!(sa.getRHS() instanceof ExprBinary)){
				return -1;
			}
			ExprBinary rhsbin = (ExprBinary) sa.getRHS();

			Integer rhsrhs = rhsbin.getRight().getIValue();
			if(rhsbin.getOp() != ExprBinary.BINOP_ADD || rhsrhs == null || rhsrhs != 1 || !rhsbin.getLeft().toString().equals(indName)){
				return -1;
			}
		}else{
			if(stmt.getIncr() instanceof StmtExpr){
				StmtExpr se = (StmtExpr) stmt.getIncr();
				if(se.getExpression() instanceof ExprUnary &&
						( ((ExprUnary)se.getExpression()).getOp() == ExprUnary.UNOP_POSTINC
								||((ExprUnary)se.getExpression()).getOp() == ExprUnary.UNOP_PREINC )){

				}else{
					return -1;
				}
			}else{
				return -1;
			}
		}
		
		return sz;

	}
	
	public boolean unrollThisLoop(StmtFor stmt){
		return true;
	}
	
	
	@Override
	public Object visitStmtFor(StmtFor stmt)
    {
		int sz = decideForLoop(stmt);
		
		if(sz < 0){
			return super.visitStmtFor(stmt);
		}
		
		StmtVarDecl svd = (StmtVarDecl)stmt.getInit();
		String indName = svd.getName(0);
		Expression init = svd.getInit (0);
		Integer low = svd.getInit(0).getIValue();
		
		if(!unrollThisLoop(stmt)){
			return super.visitStmtFor(stmt);
		}
		
		addStatement(svd);
		for(int i=0; i<sz; ++i){
			VarReplacer vr = new VarReplacer(indName, new ExprConstInt(i + low) );
			Statement s = (Statement) stmt.getBody().accept(vr);
			s = s.doStatement(this);
			addStatement( s );
			addStatement(stmt.getIncr());
		}
		return null;
    }

}
