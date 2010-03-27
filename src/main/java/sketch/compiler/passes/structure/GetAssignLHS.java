package sketch.compiler.passes.structure;

import sketch.compiler.ast.core.FETypedVisitor;
import sketch.compiler.ast.core.exprs.ExprArrayRange;
import sketch.compiler.ast.core.exprs.ExprField;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.stmts.StmtAssign;

/**
 * get the assigned left hand side
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
public class GetAssignLHS extends FETypedVisitor<ExprVar> {
    @Override
    public ExprVar visitStmtAssign(StmtAssign stmt) {
        return (ExprVar) stmt.getLHS().accept(this);
    }

    @Override
    public ExprVar visitExprField(ExprField exp) {
        return (ExprVar) exp.getLeft().accept(this);
    }

    @Override
    public ExprVar visitExprArrayRange(ExprArrayRange exp) {
        return (ExprVar) exp.getBase().accept(this);
    }

    @Override
    public ExprVar visitExprVar(ExprVar exp) {
        return exp;
    }
}
