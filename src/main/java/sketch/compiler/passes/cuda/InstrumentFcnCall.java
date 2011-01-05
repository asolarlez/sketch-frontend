package sketch.compiler.passes.cuda;

import static sketch.util.DebugOut.assertFalse;

import java.util.List;

import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.cuda.exprs.CudaInstrumentCall;
import sketch.compiler.passes.annotations.CompilerPassDeps;
import sketch.compiler.passes.preprocessing.BuiltinFcnCall;

@CompilerPassDeps(runsBefore = {}, runsAfter = {})
public class InstrumentFcnCall extends BuiltinFcnCall {
    public InstrumentFcnCall() {
        super("instrument", 2);
    }

    @Override
    public Object builtinReplacement(ExprFunCall call, List<Expression> args) {
        if (!(args.get(0) instanceof ExprVar && args.get(1) instanceof ExprVar)) {
            assertFalse("InstrumentFcnCall takes two args, the array to "
                    + "instrument and instrumentation name to use");
        }
        return new CudaInstrumentCall(call, (ExprVar) args.get(0),
                ((ExprVar) args.get(1)).getName());
    }
}
