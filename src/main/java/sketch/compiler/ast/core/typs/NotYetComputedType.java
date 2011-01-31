package sketch.compiler.ast.core.typs;

import sketch.compiler.ast.cuda.typs.CudaMemoryType;

public class NotYetComputedType extends Type {
    public NotYetComputedType(CudaMemoryType memtyp) {
        super(memtyp);
    }
}
