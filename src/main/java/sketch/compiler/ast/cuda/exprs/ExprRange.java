package sketch.compiler.ast.cuda.exprs;

import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEVisitor;
import sketch.compiler.ast.core.exprs.Expression;

/**
 * Range of numbers, similar to Scala syntax
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
public class ExprRange extends Expression {
    private final Expression from;
    private final Expression to;

    @SuppressWarnings("deprecation")
    public ExprRange(FEContext context, Expression from, Expression to) {
        super(context);
        this.from = from;
        this.to = to;
    }

    public ExprRange(FENode exprRange, Expression from, Expression to) {
        super(exprRange);
        this.from = from;
        this.to = to;
    }

    @Override
    public Object accept(FEVisitor v) {
        return v.visitExprRange(this);
    }

    public Expression getFrom() {
        return from;
    }

    public Expression getTo() {
        return to;
    }
}
