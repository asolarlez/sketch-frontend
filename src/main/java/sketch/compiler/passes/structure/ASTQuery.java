package sketch.compiler.passes.structure;

import java.security.InvalidParameterException;

import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEReplacer;

/**
 * Query about an AST property. Response is false unless a subclass' visit___ function
 * sets "this.result" to True.
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
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
