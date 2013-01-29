package sketch.compiler.ast.cuda.exprs;

import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.FEVisitor;
import sketch.compiler.ast.core.exprs.Expression;

/**
 * @deprecated
 * @author tim
 */
public class CudaBlockDim extends Expression {
    private final String indexName;

    @SuppressWarnings("deprecation")
    public CudaBlockDim(FEContext context, String indexName) {
        super(context);
        this.indexName = indexName;
    }

    @Override
    public String toString() {
        return "blockDim." + getIndexName();
    }

    @Override
    public Object accept(FEVisitor v) {
        return v.visitCudaBlockDim(this);
    }

    public String getIndexName() {
        return indexName;
    }
}
