package sketch.compiler.passes.optimization;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.exprs.ExprAbstractVariable;
import sketch.compiler.ast.core.exprs.ExprBinary;
import sketch.compiler.ast.core.stmts.StmtAssert;
import sketch.compiler.ast.core.stmts.StmtMinimize;

/**
 * convert minimize(expr) to assert(expr < value)
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
public class AbstractCostFcnAssert extends FEReplacer {
    public AbstractCostFcnAssert() {
    }

    @Override
    public Object visitStmtMinimize(StmtMinimize stmtMinimize) {
        return new StmtAssert(new ExprBinary(stmtMinimize.getMinimizeExpr(), "<",
                new ExprAbstractVariable(stmtMinimize, "MINVAR")), false);
    }
}
