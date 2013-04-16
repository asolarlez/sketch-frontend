package sketch.compiler.ast.spmd.exprs;
import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.FEVisitor;
import sketch.compiler.ast.core.exprs.Expression;

public class SpmdNProc extends Expression {
    @SuppressWarnings("deprecation")
    public SpmdNProc(FEContext context) {
        super(context);
    }

    @Override
    public Object accept(FEVisitor v) {
        return v.visitSpmdNProc(this);
    }

    @Override
    public String toString() {
        return "spmdnproc";
    }
}
