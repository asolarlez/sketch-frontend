package sketch.compiler.passes.lowering;

import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.SymbolTable;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.ExprArrayRange;
import sketch.compiler.ast.core.exprs.ExprArrayRange.RangeLen;
import sketch.compiler.ast.core.exprs.ExprBinary;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprUnary;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypePrimitive;


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

    public ExtractRightShifts(SymbolTable symtab, TempVarGen varGen) {
        super(symtab);
		this.varGen = varGen;
	}


    public Object visitExprArrayRange(ExprArrayRange exp) {
        if (exp.getBase() instanceof ExprBinary || exp.getBase() instanceof ExprUnary) {
            Type t = this.getType(exp.getBase());
            FENode context = exp;
            String tmpName = varGen.nextVar();
            Expression bexp = this.doExpression(exp.getBase());
            StmtVarDecl decl = new StmtVarDecl(context, t, tmpName, bexp);
            this.addStatement(decl);
            RangeLen range = exp.getSelection();
            Expression newStart = doExpression(range.start());
            Expression newLen = null;
            if (range.hasLen()) {
                newLen = doExpression(range.getLenExpression());
            }
            return new ExprArrayRange(exp, new ExprVar(exp, tmpName), new RangeLen(
                    newStart, newLen));
        }
        return super.visitExprArrayRange(exp);
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
        }
 else if (exp.getOp() == ExprBinary.BINOP_EQ &&
                (getType(exp.getLeft()).isArray() || getType(exp.getRight()).isArray()))
        {
			//added by rimoll
			FENode context = exp;
			
			String tmpName1 = varGen.nextVar();
            String tmpName2 = varGen.nextVar();
            String tmpName3 = varGen.nextVar();
			
			Expression lexp = this.doExpression(exp.getLeft());
			Expression rexp = this.doExpression(exp.getRight());
			
			Type lt = this.getType(lexp);
			Type rt = this.getType(rexp);
			Type t = this.getType(exp);
            Expression llen = ExprConstInt.one;
            Expression rlen = ExprConstInt.one;
            if (lt instanceof TypeArray) {
                llen = ((TypeArray) lt).getLength();
            }
            if (rt instanceof TypeArray) {
                rlen = ((TypeArray) rt).getLength();
            }

			Type nt = lt.leastCommonPromotion(rt);
			StmtVarDecl ldecl = new StmtVarDecl(context, nt, tmpName1, lexp);
			StmtVarDecl rdecl = new StmtVarDecl(context, nt, tmpName2, rexp);
			
			StmtVarDecl eqdecl = new StmtVarDecl(context, TypePrimitive.bittype, tmpName3,
                    new ExprBinary(exp, ExprBinary.BINOP_EQ,
                            new ExprVar(lexp, tmpName1), new ExprVar(rexp,
                                    tmpName2)));
			



			this.addStatement(ldecl);
			this.addStatement(rdecl);
			this.addStatement(eqdecl);

			//ExprBinary(context, exp.getOp(), new ExprVar(exp, tmpName1), new ExprVar(exp, tmpName2), exp.getAlias());
            return new ExprVar(exp, tmpName3);
		}
		else{
			return super.visitExprBinary(exp);
		}	
    }
}
