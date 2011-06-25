package sketch.compiler.passes.structure;

import java.security.InvalidParameterException;

import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEReplacer;

public abstract class ASTObjQuery<T> extends FEReplacer {
    protected T result;
    
    public ASTObjQuery(T initial) {
        result = initial;
    }

    public T run(Object obj) {
        if (obj instanceof FENode) {
            ((FENode) obj).accept(this);
        } else {
            throw new InvalidParameterException(
                    "ProgramQuery doesn't know how to deal with non-AST node " +
                            obj.getClass().getName());
        }
        return result;
    }
}
