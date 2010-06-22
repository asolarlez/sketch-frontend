package sketch.compiler.ast.core.exprs;

import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEVisitor;

/**
 * represents an abstract variable, like minimize, that will be dealt with by the new backend.
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you
 *          make changes, please consider contributing back!
 */
public class ExprAbstractVariable extends Expression {
    public final String name;

    public ExprAbstractVariable(FENode node, String name) {
        super(node);
        this.name = name;
    }

    @Override
    public Object accept(FEVisitor v) {
        return v.visitExprAbstractVariable(this);
    }
}
