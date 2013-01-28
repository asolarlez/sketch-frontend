package sketch.compiler.ast.cuda.exprs;

import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEVisitor;
import sketch.compiler.ast.core.exprs.Expression;
import static sketch.util.Misc.nonnull;

/**
 * Not used now. Range of numbers, similar to Scala syntax
 * 
 * @deprecated
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
public class ExprRange extends Expression {
    private final Expression from;
    private final Expression until;
    private final Expression by;

    @SuppressWarnings("deprecation")
    public ExprRange(FEContext context, Expression from, Expression until, Expression by) {
        super(context);
        this.from = nonnull(from);
        this.until = nonnull(until);
        this.by = nonnull(by);
    }

    public ExprRange(FENode exprRange, Expression from, Expression until, Expression by) {
        super(exprRange);
        this.from = nonnull(from);
        this.until = nonnull(until);
        this.by = nonnull(by);
    }

    @Override
    public Object accept(FEVisitor v) {
        return v.visitExprRange(this);
    }

    public Expression getFrom() {
        return from;
    }

    public Expression getUntil() {
        return until;
    }

    public Expression getBy() {
        return by;
    }
}
