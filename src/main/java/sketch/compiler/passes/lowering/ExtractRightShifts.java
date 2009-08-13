package sketch.compiler.passes.lowering;
import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.StreamType;
import sketch.compiler.ast.core.SymbolTable;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.ExprBinary;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.Type;

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
			FENode context = exp;
			String tmpName = varGen.nextVar();
			Expression lexp = this.doExpression(exp.getLeft());
			Expression rexp = this.doExpression(exp.getRight());
			StmtVarDecl decl = new StmtVarDecl(context, t, tmpName, lexp);
			this.addStatement(decl);


			return new ExprBinary(context, exp.getOp(), new ExprVar(exp, tmpName), rexp, exp.getAlias());
		}else{
			return super.visitExprBinary(exp);
		}
    }


}
