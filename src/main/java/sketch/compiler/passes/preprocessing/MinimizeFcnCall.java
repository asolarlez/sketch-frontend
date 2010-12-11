package sketch.compiler.passes.preprocessing;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtExpr;
import sketch.compiler.ast.core.stmts.StmtMinimize;
import sketch.util.exceptions.ExceptionAtNode;

/**
 * replace minimize(arg) with a special node
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
public class MinimizeFcnCall extends FEReplacer {
    @Override
    public Object visitFunction(Function func) {
        if (func.getName().equals("minimize")) {
            throw new ExceptionAtNode("minimize() is a reserved function name", func);
        }
        return super.visitFunction(func);
    }

    @Override
    public Object visitExprFunCall(ExprFunCall exp) {
        if (exp.getName().equals("minimize")) {
            assert exp.getParams().size() == 1 : "built-in minimize() takes 1 argument";
            return new StmtMinimize(exp.getParams().get(0), true);
        }
        return exp;
    }
    
    @Override
    public Object visitStmtExpr(StmtExpr stmt) {
        Object inner = stmt.getExpression().accept(this);
        if (inner instanceof Statement) {
            return inner;
        }
        return super.visitStmtExpr(stmt);
    }
}
