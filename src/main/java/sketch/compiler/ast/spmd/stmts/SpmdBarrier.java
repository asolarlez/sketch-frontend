package sketch.compiler.ast.spmd.stmts;
import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.FEVisitor;
import sketch.compiler.ast.core.stmts.Statement;

public class SpmdBarrier extends Statement {
    @SuppressWarnings("deprecation")
    public SpmdBarrier(FEContext context) {
        super(context);
    }

    @Override
    public Object accept(FEVisitor v) {
        return v.visitSpmdBarrier(this);
    }
}
