package sketch.compiler.passes.structure;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.stmts.StmtMinimize;

public class HasMinimize extends FEReplacer {
    private boolean hasMinimize;

    @Override
    public Object visitStmtMinimize(StmtMinimize stmtMinimize) {
        hasMinimize = true;
        return stmtMinimize;
    }

    public boolean hasMinimize() {
        return hasMinimize;
    }
}
