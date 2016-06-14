package sketch.compiler.ast.core.typs;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import sketch.compiler.ast.cuda.typs.CudaMemoryType;


public class NotYetComputedType extends Type {

    public static final NotYetComputedType singleton = new NotYetComputedType();
    public NotYetComputedType(CudaMemoryType memtyp) {
        super(memtyp);
    }

    public NotYetComputedType() {
        super(CudaMemoryType.UNDEFINED);
    }

    public String toString() {
        return "???";
    }

    public Collection<Type> getBaseTypes() {
        return Collections.singletonList((Type) this);
    }

    public Map<String, Type> unify(Type t, Set<String> names) {
        return Collections.EMPTY_MAP;
    }

    public String cleanName() {
        throw new RuntimeException("This type is not known");
    }

    @Override
    public TypeComparisonResult compare(Type that) {
        if (that instanceof NotYetComputedType) {
            return TypeComparisonResult.EQ;
        }
        return TypeComparisonResult.NEQ;
    }
}
