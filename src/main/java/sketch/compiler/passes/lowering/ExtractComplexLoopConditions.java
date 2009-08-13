package sketch.compiler.passes.lowering;
import java.util.ArrayList;
import java.util.List;

import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.ExprArrayRange;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprField;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprRegen;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.stmts.StmtDoWhile;
import sketch.compiler.ast.core.stmts.StmtFor;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.stmts.StmtWhile;
import sketch.compiler.ast.core.typs.TypePrimitive;

public class ExtractComplexLoopConditions extends FEReplacer {
	TempVarGen vgen;
	
	public ExtractComplexLoopConditions(TempVarGen vgen){
		this.vgen = vgen;
	}
	
	private boolean isComplexExpr(FENode exp){		
		class isComplex extends FEReplacer{
			boolean is = false;
			@Override
			public Object visitExprArrayRange(ExprArrayRange ar){
				is = true;
				return ar;
			}
			
			@Override
			public Object visitExprFunCall(ExprFunCall fc){
				is = true;
				return fc;
			}
			
			@Override
			public Object visitExprRegen(ExprRegen fc){
				is = true;
				return fc;
			}	
			
			public Object visitExprField(ExprField ef){
				is = true;
				return ef;
			}
		}
		isComplex ic = new isComplex();
		if(exp != null){
			exp.accept(ic);
			return ic.is;
		}else{
			return false;
		}
	}
	
	@Override
	public Object visitStmtFor(StmtFor sf){
		
		if(isComplexExpr(sf.getCond())  ||  isComplexExpr(sf.getIncr()) || isComplexExpr(sf.getInit()) ){
			List<Statement> bl = new ArrayList<Statement>();
			bl.add(sf.getInit());
			String nm = vgen.nextVar();
			bl.add(new StmtVarDecl(sf.getCond(), TypePrimitive.bittype, nm, sf.getCond()));
			
			List<Statement> lbody = new ArrayList<Statement>();
			
			lbody.add((Statement)sf.getBody().accept(this));
			lbody.add(sf.getIncr());
			Expression tmpvar = new ExprVar(sf.getCond(), nm);
			lbody.add(new StmtAssign(sf.getCond(), tmpvar, sf.getCond()));			
			StmtBlock sb = new StmtBlock(sf, lbody);
			bl.add(new StmtWhile(sf, tmpvar, sb ));
			return new StmtBlock(sf, bl);
		}else{
			return super.visitStmtFor(sf);
		}
	}
	
	@Override
	public Object visitStmtWhile(StmtWhile sw){
		
		if(isComplexExpr(sw.getCond())){
			List<Statement> bl = new ArrayList<Statement>();
			String nm = vgen.nextVar();
			bl.add(new StmtVarDecl(sw.getCond(), TypePrimitive.bittype, nm, sw.getCond()));
			List<Statement> lbody = new ArrayList<Statement>();
			
			lbody.add((Statement)sw.getBody().accept(this));
			Expression tmpvar = new ExprVar(sw.getCond(), nm);
			lbody.add(new StmtAssign(sw.getCond(), tmpvar, sw.getCond()));			
			StmtBlock sb = new StmtBlock(sw.getBody(), lbody);
			bl.add(new StmtWhile(sw, tmpvar, sb ));
			return new StmtBlock(sw, bl);			
		}else{
			return super.visitStmtWhile(sw);
		}		
	}
	
	@Override
	public Object visitStmtDoWhile(StmtDoWhile sdw){
		
		if(isComplexExpr(sdw.getCond())){
			List<Statement> bl = new ArrayList<Statement>();
			
			String nm = vgen.nextVar();
			bl.add(new StmtVarDecl(sdw.getCond(), TypePrimitive.bittype, nm, ExprConstInt.zero));
			
			List<Statement> lbody = new ArrayList<Statement>();
			
			lbody.add((Statement)sdw.getBody().accept(this));
			Expression tmpvar = new ExprVar(sdw.getCond(), nm);
			lbody.add(new StmtAssign(sdw.getCond(), tmpvar, sdw.getCond()));			
			StmtBlock sb = new StmtBlock(sdw.getBody(), lbody);
			bl.add(new StmtDoWhile(sdw, sb, tmpvar));
			return new StmtBlock(sdw, bl);			
		}else{
			return super.visitStmtDoWhile(sdw);
		}		
		
		
	}
	
	
	

}
