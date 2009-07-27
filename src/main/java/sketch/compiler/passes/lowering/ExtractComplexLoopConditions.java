package streamit.frontend.passes;

import java.util.ArrayList;
import java.util.List;

import streamit.frontend.nodes.ExprArrayRange;
import streamit.frontend.nodes.ExprConstInt;
import streamit.frontend.nodes.ExprField;
import streamit.frontend.nodes.ExprFunCall;
import streamit.frontend.nodes.ExprRegen;
import streamit.frontend.nodes.ExprVar;
import streamit.frontend.nodes.Expression;
import streamit.frontend.nodes.FENode;
import streamit.frontend.nodes.FEReplacer;
import streamit.frontend.nodes.Statement;
import streamit.frontend.nodes.StmtAssign;
import streamit.frontend.nodes.StmtBlock;
import streamit.frontend.nodes.StmtDoWhile;
import streamit.frontend.nodes.StmtFor;
import streamit.frontend.nodes.StmtVarDecl;
import streamit.frontend.nodes.StmtWhile;
import streamit.frontend.nodes.TempVarGen;
import streamit.frontend.nodes.TypePrimitive;

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
