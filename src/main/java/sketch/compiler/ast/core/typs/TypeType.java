package sketch.compiler.ast.core.typs;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static sketch.util.DebugOut.not_implemented;

import sketch.compiler.ast.cuda.typs.CudaMemoryType;

/**
 * Not used. type of a type. For example, if we have
 * 
 * <pre>
 * type v = typearray { \Z mod 4 -> \Z }
 * </pre>
 * 
 * the type of $v is TypeType
 * 
 * @deprecated
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
public class TypeType extends Type {
    protected Type base;

    public TypeType(CudaMemoryType memtyp) {
        super(memtyp);
        this.base = null;
    }

    public TypeType(CudaMemoryType memtyp, Type base) {
        this(memtyp);
        this.base = base;
    }

    public Type maybeGetBase() {
        return base;
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
        return not_implemented();
    }
}
