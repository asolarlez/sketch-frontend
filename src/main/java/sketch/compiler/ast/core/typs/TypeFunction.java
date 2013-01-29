package sketch.compiler.ast.core.typs;

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

    public String toString() {
        return "fun";
    }

}
