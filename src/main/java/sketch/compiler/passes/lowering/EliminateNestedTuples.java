package sketch.compiler.passes.lowering;

import java.util.ArrayList;
import java.util.List;

import sketch.compiler.ast.core.SymbolTable;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.ExprTuple;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.TypeStructRef;

public class EliminateNestedTuples extends SymbolTableVisitor {
    private TempVarGen varGen;

    public EliminateNestedTuples(TempVarGen varGen) {
        super(null);
        this.varGen = varGen;
    }

    public EliminateNestedTuples(SymbolTable symtab, TempVarGen varGen) {
        super(symtab);
        this.varGen = varGen;
    }

    public Object visitExprTuple(ExprTuple exp) {
        List<Expression> newElements = new ArrayList<Expression>();
        for (Expression e : exp.getElements()) {
            Expression newExp = this.doExpression(e);
            if (newExp instanceof ExprTuple) {
                String tmpName = varGen.nextVar();
                StmtVarDecl decl =
                        new StmtVarDecl(e.getContext(), new TypeStructRef(
                                ((ExprTuple) newExp).getName(), false), tmpName, newExp);
                this.addStatement(decl);
                newElements.add(new ExprVar(e, tmpName));
            } else {
                newElements.add(newExp);
            }
        }

        return new ExprTuple(exp, newElements, exp.getName());
    }


}
