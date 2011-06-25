package sketch.compiler.ast.core.exprs;

import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEVisitor;
import sketch.compiler.ast.core.typs.Type;
import static sketch.util.Misc.nonnull;

/**
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
public class ExprType extends Expression {
    private final Type typ;

    public ExprType(FENode node, Type typ) {
        super(node);
        this.typ = nonnull(typ);
    }

    @Override
    public Object accept(FEVisitor v) {
        return v.visitExprType(this);
    }

    public Type getType() {
        return typ;
    }
}
