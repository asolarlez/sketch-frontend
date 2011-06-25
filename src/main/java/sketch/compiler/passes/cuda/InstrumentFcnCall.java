package sketch.compiler.passes.cuda;

import java.util.List;

import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.cuda.exprs.CudaInstrumentCall;
import sketch.compiler.passes.annotations.CompilerPassDeps;
import sketch.compiler.passes.preprocessing.BuiltinFcnCall;

import static sketch.util.DebugOut.assertFalse;

/**
 * builtin function call to instrument a variable
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
@CompilerPassDeps(runsBefore = {}, runsAfter = {})
public class InstrumentFcnCall extends BuiltinFcnCall {
    public InstrumentFcnCall() {
        super("instrument", 3);
    }

    public Object builtinReplacement(ExprFunCall call, Expression toInstrument,
            Expression instrVar, Expression instrClassName)
    {
        if (!(toInstrument instanceof ExprVar && instrClassName instanceof ExprVar)) {
            assertFalse("InstrumentFcnCall takes three args, the array to "
                    + "instrument, an instrumentation variable, and "
                    + "instrumentation name to use");
        }
        return new CudaInstrumentCall(call, (ExprVar) toInstrument, (ExprVar) instrVar,
                ((ExprVar) instrClassName).getName());
    }

    @Override
    public Object builtinReplacement(ExprFunCall call, List<Expression> args) {
        return builtinReplacement(call, args.get(0), args.get(1), args.get(2));
    }
}
