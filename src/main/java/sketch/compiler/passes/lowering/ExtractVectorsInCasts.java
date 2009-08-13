package sketch.compiler.passes.lowering;
import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.StreamType;
import sketch.compiler.ast.core.SymbolTable;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.ExprTypeCast;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.Type;

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
