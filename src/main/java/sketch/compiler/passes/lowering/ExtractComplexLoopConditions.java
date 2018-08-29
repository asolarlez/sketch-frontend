package sketch.compiler.passes.lowering;
import java.util.ArrayList;
import java.util.List;

import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.SymbolTable;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.ExprArrayRange;
import sketch.compiler.ast.core.exprs.ExprBinary;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprField;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprUnary;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.exprs.regens.ExprRegen;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.stmts.StmtDoWhile;
import sketch.compiler.ast.core.stmts.StmtFor;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.stmts.StmtWhile;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypePrimitive;

// FIXME xzl: temporarily disabled to help stencil
public class ExtractComplexLoopConditions extends SymbolTableVisitor {
	TempVarGen vgen;
	
	public ExtractComplexLoopConditions(TempVarGen vgen){
		super(null);
		this.vgen = vgen;
	}
	
	private boolean isComplexExpr(FENode exp, final boolean unaryok) {
		class isComplex extends FEReplacer{
			boolean is = false;
			@Override
			public Object visitExprArrayRange(ExprArrayRange ar){
				is = true;
				return ar;
			}
			
			public Object visitExprBinary(ExprBinary exp) {
				if (exp.getOp() == ExprBinary.BINOP_EQ) {
					Type t = getType(exp.getLeft());
					if (t.isArray()) {
						is = true;
						return exp;
					}
					t = getType(exp.getRight());
					if (t.isArray()) {
						is = true;
						return exp;
					}
				}
				exp.getLeft().accept(this);
				exp.getRight().accept(this);
				return exp;
			}

			public Object visitExprUnary(ExprUnary exp) {
				if (unaryok) {
					return super.visitExprUnary(exp);
				}
				switch (exp.getOp()) {
				case ExprUnary.UNOP_POSTDEC:
				case ExprUnary.UNOP_POSTINC:
				case ExprUnary.UNOP_PREDEC:
				case ExprUnary.UNOP_PREINC:
					is = true;
					return exp;
				default:
					return super.visitExprUnary(exp);

				}
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
	boolean isStencil = false;
	
	@Override
	public Object visitFunction(Function f){
	    isStencil = f.isStencil();
	    return super.visitFunction(f);	    
	}
	
	
	@Override
	public Object visitStmtFor(StmtFor sf){

        if (sf.isCanonical()) {
            return super.visitStmtFor(sf);
        }

		SymbolTable oldSymTab = symtab;
		symtab = new SymbolTable(symtab);

		if (sf.getInit() != null) {
			sf.getInit().accept(this);
		}
		
		if (isComplexExpr(sf.getCond(), false) || isComplexExpr(sf.getIncr(), true)
				|| isComplexExpr(sf.getInit(), true)) {
		    
			
			String nm = vgen.nextVar();
			
			Expression tmpvar = new ExprVar(sf.getCond(), nm);
			
			List<Statement> bl = new ArrayList<Statement>();
			if(isStencil){
			    assert sf.getCond() instanceof ExprBinary : "For stencils, all loop conditions must be of the form i < X where X is loop invariant";
			    ExprBinary cnd = (ExprBinary) sf.getCond();
			    assert cnd.getOp() == ExprBinary.BINOP_LE || cnd.getOp() == ExprBinary.BINOP_LT :  "For stencils, all loop conditions must be of the form i < X or i<= X where X is loop invariant";
			    assert cnd.getLeft() instanceof ExprVar : "For stencils, all loop conditions must be of the form i < X where X is loop invariant";
			    
			    bl.add(new StmtVarDecl(sf.getCond(), TypePrimitive.inttype, nm, cnd.getRight()));
			    bl.add(new StmtFor(sf, sf.getInit(), new ExprBinary(cnd, cnd.getOp(), cnd.getLeft(), tmpvar), sf.getIncr(), (Statement)sf.getBody().accept(this), true));
			}else{
			    List<Statement> lbody = new ArrayList<Statement>();
	            
	            lbody.add((Statement)sf.getBody().accept(this));
	            lbody.add(sf.getIncr());
	            
	            lbody.add(new StmtAssign(sf.getCond(), tmpvar, sf.getCond()));         
	            StmtBlock sb = new StmtBlock(sf, lbody);
	            
                bl.add(sf.getInit());
                bl.add(new StmtVarDecl(sf.getCond(), TypePrimitive.bittype, nm, sf.getCond()));
    			bl.add(new StmtWhile(sf, tmpvar, sb ));
			}
			symtab = oldSymTab;
			return new StmtBlock(sf, bl);
		}else{
			symtab = oldSymTab;
			return super.visitStmtFor(sf);
		}
	}
	
	@Override
	public Object visitStmtWhile(StmtWhile sw){
		
		if (isComplexExpr(sw.getCond(), false)) {
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
		
		if (isComplexExpr(sdw.getCond(), false)) {
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
