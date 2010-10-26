package sketch.compiler.ast.cuda.stmts;

import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.FEVisitor;
import sketch.compiler.ast.core.stmts.Statement;

public class CudaSyncthreads extends Statement {
    @SuppressWarnings("deprecation")
    public CudaSyncthreads(FEContext context) {
        super(context);
    }

    @Override
    public Object accept(FEVisitor v) {
        return v.visitCudaSyncthreads(this);
    }
}
