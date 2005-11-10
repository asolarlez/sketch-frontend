package streamit.frontend.passes;

import streamit.frontend.nodes.ExprBinary;
import streamit.frontend.nodes.ExprTypeCast;
import streamit.frontend.nodes.ExprVar;
import streamit.frontend.nodes.Expression;
import streamit.frontend.nodes.FEContext;
import streamit.frontend.nodes.StmtVarDecl;
import streamit.frontend.nodes.StreamType;
import streamit.frontend.nodes.SymbolTable;
import streamit.frontend.nodes.TempVarGen;
import streamit.frontend.nodes.Type;

public class ExtractRightShifts extends SymbolTableVisitor {
	private TempVarGen varGen;
	public ExtractRightShifts(TempVarGen varGen) {
		super(null);
		this.varGen = varGen;
	}

	public ExtractRightShifts(SymbolTable symtab, StreamType st, TempVarGen varGen) {
		super(symtab, st);
		this.varGen = varGen;
	}
	
	
	public Object visitExprBinary(ExprBinary exp)
    {
		if(exp.getOp() == ExprBinary.BINOP_RSHIFT){
			Type t = this.getType(exp);
			FEContext context = exp.getContext();
			String tmpName = varGen.nextVar();
			Expression lexp = this.doExpression(exp.getLeft());
			Expression rexp = this.doExpression(exp.getRight());
			ExprBinary nexp = exp;
			if(lexp != exp.getLeft() || rexp != exp.getRight()){
				nexp = new ExprBinary(context, exp.getOp(), lexp, rexp);
			}
			StmtVarDecl decl = new StmtVarDecl(context, t, tmpName, nexp);
			this.addStatement(decl);
			return new ExprVar(context, tmpName);
		}else{
			return super.visitExprBinary(exp);
		}        
    }
	

}
