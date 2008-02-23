package streamit.frontend.passes;

import streamit.frontend.nodes.ExprBinary;
import streamit.frontend.nodes.ExprTypeCast;
import streamit.frontend.nodes.ExprVar;
import streamit.frontend.nodes.Expression;
import streamit.frontend.nodes.FEContext;
import streamit.frontend.nodes.FENode;
import streamit.frontend.nodes.StmtVarDecl;
import streamit.frontend.nodes.StreamType;
import streamit.frontend.nodes.SymbolTable;
import streamit.frontend.nodes.TempVarGen;
import streamit.frontend.nodes.Type;

public class ExtractVectorsInCasts extends SymbolTableVisitor {

	private TempVarGen varGen;
	public ExtractVectorsInCasts(TempVarGen varGen) {
		super(null);
		this.varGen = varGen;
	}

	public ExtractVectorsInCasts(SymbolTable symtab, StreamType st, TempVarGen varGen) {
		super(symtab, st);
		this.varGen = varGen;
	}


    public Object visitExprTypeCast(ExprTypeCast exp)
    {
    	if(!(exp.getExpr() instanceof ExprVar)){
    		Type t = this.getType(exp.getExpr());
			FENode context = exp;
			String tmpName = varGen.nextVar();
			Expression lexp = this.doExpression(exp.getExpr());
			StmtVarDecl decl = new StmtVarDecl(context, t, tmpName, lexp);
			this.addStatement(decl);
			return new ExprTypeCast(context, exp.getType(), new ExprVar(context, tmpName));
    	}
        return super.visitExprTypeCast(exp);
    }

}
