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

/**
 * Actually, this extracts both right shifts and left shifts.
 * Without this transformation, GenerateCopies produces buggy code.
 * @author asolar
 *
 */


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
		if(exp.getOp() == ExprBinary.BINOP_RSHIFT || exp.getOp() == ExprBinary.BINOP_LSHIFT){
			Type t = this.getType(exp);
			FEContext context = exp.getCx();
			String tmpName = varGen.nextVar();
			Expression lexp = this.doExpression(exp.getLeft());
			Expression rexp = this.doExpression(exp.getRight());			
			StmtVarDecl decl = new StmtVarDecl(context, t, tmpName, lexp);
			this.addStatement(decl);
			
			
			return new ExprBinary(context, exp.getOp(), new ExprVar(null, tmpName), rexp, exp.getAlias());
		}else{
			return super.visitExprBinary(exp);
		}        
    }
	

}
