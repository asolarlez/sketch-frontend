package sketch.compiler.ast.scala.exprs;

import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.FEVisitor;
import sketch.compiler.ast.core.exprs.ExprConstant;
import sketch.util.DebugOut;

/**
 * created for Scala translation only.
 * @author gatoatigrado
 */
public class ExprConstUnit extends ExprConstant {
    public ExprConstUnit(FEContext context) {
        super(context);
    }

    @Override
    public Object accept(FEVisitor v) {
        DebugOut.assertFalse("ExprConstUnit should have been erased.");
        return null;
    }
}
