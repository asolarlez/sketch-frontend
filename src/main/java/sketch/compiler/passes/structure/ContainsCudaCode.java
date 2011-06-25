package sketch.compiler.passes.structure;

import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.cuda.stmts.CudaSyncthreads;

public class ContainsCudaCode extends ASTQuery {
    @Override
    public Object visitCudaSyncthreads(CudaSyncthreads cudaSyncthreads) {
        this.result = true;
        return super.visitCudaSyncthreads(cudaSyncthreads);
    }
    
    @Override
    public Object visitFunction(Function func) {
        // TODO Auto-generated method stub
        return super.visitFunction(func);
    }
}
