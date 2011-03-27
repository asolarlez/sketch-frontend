package sketch.compiler.passes.cuda;

import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.StmtReturn;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.cuda.typs.CudaMemoryType;
import sketch.compiler.passes.annotations.CompilerPassDeps;
import sketch.compiler.passes.lowering.SymbolTableVisitor;

@CompilerPassDeps(runsBefore = {}, runsAfter = {})
public class CopyCudaMemTypeToFcnReturn extends SymbolTableVisitor {
    public CudaMemoryType returnType;
    
    public CopyCudaMemTypeToFcnReturn() {
        super(null);
    }

    @Override
    public Object visitFunction(Function func) {
        returnType = null;
        func = (Function) super.visitFunction(func);
        if (returnType == null) {
            return func;
        } else {
            Type nextType = func.getReturnType().withMemType(returnType);
            return func.creator().returnType(nextType).create();
        }
    }
    
    @Override
    public Object visitStmtReturn(StmtReturn stmt) {
        final Expression rv = stmt.getValue();
        final Type retTyp = this.getType(rv);
        final CudaMemoryType cudaMemType = retTyp.getCudaMemType();
        if (cudaMemType != CudaMemoryType.UNDEFINED) {
            this.returnType = cudaMemType;
        }
        return super.visitStmtReturn(stmt);
    }
}
