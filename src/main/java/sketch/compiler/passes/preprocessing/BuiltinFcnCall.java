package sketch.compiler.passes.preprocessing;

import java.util.List;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtExpr;
import sketch.util.exceptions.ExceptionAtNode;

/**
 * replace a certain function call
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you
 *          make changes, please consider contributing back!
 */
public abstract class BuiltinFcnCall extends FEReplacer {
    protected final String name;
    protected final int numArgs;

    public BuiltinFcnCall(String name, int numArgs) {
        this.name = name;
        this.numArgs = numArgs;
    }
    
    @Override
    public Object visitFunction(Function func) {
        if (func.getName().equals(name)) {
            throw new ExceptionAtNode(name + "() is a reserved function name", func);
        }
        return super.visitFunction(func);
    }

    @Override
    public Object visitExprFunCall(ExprFunCall exp) {
        if (exp.getName().equals(name)) {
            if (numArgs >= 0) {
                assert exp.getParams().size() == numArgs : "built-in " + name +
                        "() takes " + numArgs + "argument(s)";
            }
            return builtinReplacement(exp, exp.getParams());
        }
        return exp;
    }
    
    /** sometimes, expressions are converted to statements */
    @Override
    public Object visitStmtExpr(StmtExpr stmt) {
        Object inner = stmt.getExpression().accept(this);
        if (inner instanceof Statement) {
            return inner;
        }
        return super.visitStmtExpr(stmt);
    }
    
    public abstract Object builtinReplacement(ExprFunCall call, List<Expression> args);
}
