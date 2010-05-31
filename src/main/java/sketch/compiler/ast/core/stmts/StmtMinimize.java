package sketch.compiler.ast.core.stmts;

import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.FEVisitor;
import sketch.compiler.ast.core.exprs.Expression;

public class StmtMinimize extends Statement {
    private final Expression minimizeExpr;

    @SuppressWarnings("deprecation")
    public StmtMinimize(Expression minimizeExpr) {
        super(FEContext.artificalFrom("minimize", minimizeExpr));
        this.minimizeExpr = minimizeExpr;
    }

    @Override
    public Object accept(FEVisitor v) {
        return v.visitStmtMinimize(this);
    }

    public Expression getMinimizeExpr() {
        return minimizeExpr;
    }
}
