package streamit.frontend.passes;

import java.util.ArrayList;
import java.util.List;

import streamit.frontend.nodes.ExprBinary;
import streamit.frontend.nodes.ExprConstInt;
import streamit.frontend.nodes.ExprUnary;
import streamit.frontend.nodes.ExprVar;
import streamit.frontend.nodes.FEReplacer;
import streamit.frontend.nodes.Statement;
import streamit.frontend.nodes.StmtAssign;
import streamit.frontend.nodes.StmtBlock;
import streamit.frontend.nodes.StmtExpr;
import streamit.frontend.nodes.StmtFor;
import streamit.frontend.nodes.StmtVarDecl;
import streamit.frontend.nodes.TypePrimitive;
import streamit.frontend.stencilSK.VarReplacer;

public class SimpleLoopUnroller extends FEReplacer {
	
	@Override
	public Object visitStmtFor(StmtFor stmt)
    {
		if(!(stmt.getInit() instanceof StmtVarDecl)){
			return super.visitStmtFor(stmt);
		}
		StmtVarDecl svd = (StmtVarDecl)stmt.getInit();
		if(svd.getNumVars() != 1){
			return super.visitStmtFor(stmt);
		}
		Integer low = svd.getInit(0).getIValue();
		if(low == null){
			return super.visitStmtFor(stmt);
		}
		String indName = svd.getName(0);
		if(!( stmt.getCond() instanceof ExprBinary )  ){
			return super.visitStmtFor(stmt);
		}
		ExprBinary eb = (ExprBinary) stmt.getCond();
		if(eb.getOp() != ExprBinary.BINOP_LE && eb.getOp() != ExprBinary.BINOP_LT){
			return super.visitStmtFor(stmt);
		}
		if( ! eb.getLeft().toString().equals(indName)  ){
			return super.visitStmtFor(stmt);
		}
		Integer high = eb.getRight().getIValue();
		if(high == null){
			return super.visitStmtFor(stmt);
		}
		int sz = high - low;
		if(  eb.getOp() == ExprBinary.BINOP_LE ){
			sz++;
		}
		
		if(stmt.getIncr() instanceof StmtAssign){
			StmtAssign sa = (StmtAssign)stmt.getIncr();
			if(!sa.getLHS().toString().equals(indName)){
				return super.visitStmtFor(stmt);
			}
			
			if(!(sa.getRHS() instanceof ExprBinary)){
				return super.visitStmtFor(stmt);
			}
			ExprBinary rhsbin = (ExprBinary) sa.getRHS();
			
			Integer rhsrhs = rhsbin.getRight().getIValue();
			if(rhsbin.getOp() != ExprBinary.BINOP_ADD || rhsrhs == null || rhsrhs != 1 || !rhsbin.getLeft().toString().equals(indName)){
				return super.visitStmtFor(stmt);
			}
		}else{
			if(stmt.getIncr() instanceof StmtExpr){
				StmtExpr se = (StmtExpr) stmt.getIncr();
				if(se.getExpression() instanceof ExprUnary && 
						( ((ExprUnary)se.getExpression()).getOp() == ExprUnary.UNOP_POSTINC 
								||((ExprUnary)se.getExpression()).getOp() == ExprUnary.UNOP_PREINC )){
					
				}else{
					return super.visitStmtFor(stmt);	
				}
			}else{
				return super.visitStmtFor(stmt);
			}
		}
		
		
		for(int i=0; i<sz; ++i){
			VarReplacer vr = new VarReplacer(indName, new ExprConstInt(i + low) );
			Statement s = (Statement) stmt.getBody().accept(vr);
			s = s.doStatement(this);
			addStatement( s );
		}
		return null;
    }

}
