package sketch.compiler.ast.core.exprs;

import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEVisitor;
import sketch.compiler.ast.core.typs.StructDef;

/**
 * Named parameter for a call to {@link ExprNew} constructor; used for initializing each
 * field of a {@link StructDef} object.
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
public class ExprNamedParam extends Expression {
    private final String name;
    private final Expression expr;

    @SuppressWarnings("deprecation")
    public ExprNamedParam(FEContext context, String name, Expression expr) {
        super(context);
        this.name = name;
        this.expr = expr;
    }

    public ExprNamedParam(FENode prev, String name, Expression expr) {
        super(prev);
        this.name = name;
        this.expr = expr;
    }
    
    @Override
    public String toString() {
        return name + "=" + expr;
    }

    @Override
    public Object accept(FEVisitor v) {
        return v.visitExprNamedParam(this);
    }

    public String getName() {
        return name;
    }

    public Expression getExpr() {
        return expr;
    }

    public ExprNamedParam next(Expression sub) {
        return new ExprNamedParam(this, this.name, sub);
    }

    @Override
    public Integer getIValue() {
        return this.getExpr().getIValue();
    }
}
