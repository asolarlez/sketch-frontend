package sketch.compiler.ast.core.exprs;

import java.util.Arrays;
import java.util.List;

import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEVisitor;
import sketch.util.datastructures.TprintTuple;

/**
 * List of print tuples
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
public class ExprTprint extends Expression {
    public final List<TprintTuple> expressions;

    @SuppressWarnings("deprecation")
    public ExprTprint(FEContext context, List<TprintTuple> expressions) {
        super(context);
        this.expressions = expressions;
    }

    public ExprTprint(FENode context, List<TprintTuple> expressions) {
        super(context);
        this.expressions = expressions;
    }

    public ExprTprint(FENode context, TprintTuple... expressions) {
        super(context);
        this.expressions = Arrays.asList(expressions);
    }

    @Override
    public Object accept(FEVisitor v) {
        return v.visitExprTprint(this);
    }
}
