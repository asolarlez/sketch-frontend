package sketch.compiler.ast.core.typs;

import sketch.compiler.ast.cuda.typs.CudaMemoryType;


public class NotYetComputedType extends Type {
    public NotYetComputedType(CudaMemoryType memtyp) {
        super(memtyp);
    }

    public NotYetComputedType() {
        super(CudaMemoryType.UNDEFINED);
    }
    @Override
    public TypeComparisonResult compare(Type that) {
        if (that instanceof NotYetComputedType) {
            return TypeComparisonResult.EQ;
        }
        return TypeComparisonResult.NEQ;
    }
}
