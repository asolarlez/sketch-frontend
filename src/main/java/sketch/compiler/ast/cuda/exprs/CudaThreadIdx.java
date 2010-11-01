package sketch.compiler.ast.cuda.exprs;

import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.FEVisitor;
import sketch.compiler.ast.core.exprs.Expression;

/**
 * Thread indices for CUDA programs
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
public class CudaThreadIdx extends Expression {
    private final String indexName;

    @SuppressWarnings("deprecation")
    public CudaThreadIdx(FEContext context, String indexName) {
        super(context);
        this.indexName = indexName;
    }

    @Override
    public String toString() {
        return "threadIdx." + indexName;
    }
    
    @Override
    public Object accept(FEVisitor v) {
        return v.visitCudaThreadIdx(this);
    }

    public String getIndexName() {
        return indexName;
    }
}
