package sketch.compiler.ast.core.typs;

import sketch.compiler.ast.cuda.typs.CudaMemoryType;


public class NotYetComputedType extends Type {
    public NotYetComputedType(CudaMemoryType memtyp) {
        super(memtyp);
    }

    public NotYetComputedType() {
        super(CudaMemoryType.UNDEFINED);
    }

    public String toString() {
        return "???";
    }

    @Override
    public TypeComparisonResult compare(Type that) {
        if (that instanceof NotYetComputedType) {
            return TypeComparisonResult.EQ;
        }
        return TypeComparisonResult.NEQ;
    }
}
