package sketch.compiler.passes.lowering;

import sketch.compiler.ast.core.SymbolTable;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.ExprTupleAccess;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.Type;

public class EliminateNestedTupleReads extends SymbolTableVisitor {
    private TempVarGen varGen;

    public EliminateNestedTupleReads(TempVarGen varGen) {
        super(null);
        this.varGen = varGen;
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

}
