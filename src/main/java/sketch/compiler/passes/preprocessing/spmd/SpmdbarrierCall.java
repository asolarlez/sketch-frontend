package sketch.compiler.passes.preprocessing.spmd;

import java.util.List;

import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.spmd.stmts.SpmdBarrier;
import sketch.compiler.passes.annotations.CompilerPassDeps;
import sketch.compiler.passes.preprocessing.BuiltinFcnCall;

/**
 * replace spmdbarrier() with a special node
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
@CompilerPassDeps(runsBefore = {}, runsAfter = {})
public class SpmdbarrierCall extends BuiltinFcnCall {
    static final String usageStr = "spmdbarrier() usage: spmdbarrier()";

    public SpmdbarrierCall() {
        super("spmdbarrier", 0);
    }

    @Override
    public Object builtinReplacement(ExprFunCall ctx, List<Expression> args) {
        return new SpmdBarrier(ctx.getCx());
    }
}
