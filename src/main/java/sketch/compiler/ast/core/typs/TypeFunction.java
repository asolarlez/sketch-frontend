package sketch.compiler.ast.core.typs;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * A function type. This is used for example to pass a function as a parameter to another
 * function. This is often used to construct a higher-order generator.
 */
public class TypeFunction extends Type {

    public static TypeFunction singleton = new TypeFunction();

    private TypeFunction() {
        super(null);
    }

    @Override
    public TypeComparisonResult compare(Type that) {
        // TODO Auto-generated method stub
        return null;
    }

    public Collection<Type> getBaseTypes() {
        return Collections.singletonList((Type) this);
    }

    public String toString() {
        return "fun";
    }

    public String cleanName() {
		return "___";
		// throw new RuntimeException("This type is not known");
    }

    public Map<String, Type> unify(Type t, Set<String> names) {
        return Collections.EMPTY_MAP;
    }

}
