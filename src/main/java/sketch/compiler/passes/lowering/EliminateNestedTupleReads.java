package sketch.compiler.passes.lowering;

import sketch.compiler.ast.core.SymbolTable;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.ExprArrayRange;
import sketch.compiler.ast.core.exprs.ExprArrayRange.RangeLen;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprTupleAccess;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;

public class EliminateNestedTupleReads extends SymbolTableVisitor {
    private TempVarGen varGen;
    private Expression maxArrSize;

    public EliminateNestedTupleReads(TempVarGen varGen, Expression maxArrSize) {
        super(null);
        this.varGen = varGen;
        this.maxArrSize = maxArrSize;
    }

    public EliminateNestedTupleReads(SymbolTable symtab, TempVarGen varGen) {
        super(symtab);
        this.varGen = varGen;
    }

    public Object visitExprTupleAccess(ExprTupleAccess exp) {
        Expression newBase = this.doExpression(exp.getBase());
        if (!(newBase instanceof ExprVar)) {
            String tmpName = varGen.nextVar();
            Type t = this.getType(exp.getBase());
            StmtVarDecl decl = new StmtVarDecl(exp.getContext(), t, tmpName, newBase);
            this.addStatement(decl);
            return new ExprTupleAccess(exp, new ExprVar(exp, tmpName), exp.getIndex());
        } else {
            return new ExprTupleAccess(exp, newBase, exp.getIndex());
        }

    }

    public Object visitExprArrayRange(ExprArrayRange exp) {
        if (exp.getBase() instanceof ExprTupleAccess) {
            Expression newBase = this.doExpression(exp.getBase());
            String tmpName = varGen.nextVar();
            Type t = this.getType(exp.getBase());
            if (t.isArray()) {
                TypeArray ta = (TypeArray) t;
                if (!(ta.getLength() instanceof ExprConstInt)) {
                    t = new TypeArray((Type) ta.getBase().accept(this), maxArrSize);
                }
            }
            StmtVarDecl decl = new StmtVarDecl(exp.getContext(), t, tmpName, newBase);
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
        return exp;
    }

}
