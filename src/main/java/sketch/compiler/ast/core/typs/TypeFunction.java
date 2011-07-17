package sketch.compiler.ast.core.typs;

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

}
