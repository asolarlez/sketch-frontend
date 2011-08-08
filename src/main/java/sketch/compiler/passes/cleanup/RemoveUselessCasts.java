package sketch.compiler.passes.cleanup;

import sketch.compiler.ast.core.exprs.ExprTypeCast;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.passes.lowering.SymbolTableVisitor;

public class RemoveUselessCasts extends SymbolTableVisitor {

    public RemoveUselessCasts() {
        super(null);
    }

    public Object visitExprTypeCast(ExprTypeCast etc) {
        Expression expr = (Expression) etc.getExpr().accept(this);
        Type t1 = getType(expr);
        if (t1.equals(etc.getType())) {
            return expr;
        }
        return super.visitExprTypeCast(etc);
    }

}
