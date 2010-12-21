package sketch.compiler.passes.structure;

import java.security.InvalidParameterException;

import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEReplacer;

public abstract class ASTQuery extends FEReplacer {
    protected boolean result = false;

    public boolean run(Object obj) {
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
