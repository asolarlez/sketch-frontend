package sketch.compiler.passes.preprocessing;

import java.util.List;

import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.StmtMinimize;
import sketch.compiler.passes.annotations.CompilerPassDeps;

/**
 * replace minimize(arg) with a special node
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
@CompilerPassDeps(runsAfter = {}, runsBefore = {})
public class MinimizeFcnCall extends BuiltinFcnCall {
    public MinimizeFcnCall() {
        super("minimize", 1);
    }

    @Override
    public Object builtinReplacement(ExprFunCall call, List<Expression> args) {
        return new StmtMinimize(args.get(0), true);
    }
}
