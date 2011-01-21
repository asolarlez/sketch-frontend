package sketch.compiler.passes.preprocessing;

import java.util.List;
import java.util.Vector;

import sketch.compiler.ast.core.exprs.ExprConstStr;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprTprint;
import sketch.compiler.ast.core.exprs.ExprTprint.CudaType;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.passes.annotations.CompilerPassDeps;
import sketch.util.datastructures.TprintTuple;

/**
 * replace minimize(arg) with a special node
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
@CompilerPassDeps(runsAfter = {}, runsBefore = {})
public class TprintFcnCall extends BuiltinFcnCall {
    static final String usageStr =
            "tprint usage: tprint(\"identifier 1\", value 1, \"identifier 2\", value 2, ...)";
    protected CudaType cuda_type = CudaType.Unknown;

    public TprintFcnCall() {
        this("tprint");
    }

    protected TprintFcnCall(String name) {
        super(name, -1);
    }

    @Override
    public Object builtinReplacement(ExprFunCall ctx, List<Expression> args) {
        Vector<TprintTuple> tprintArgs = new Vector<TprintTuple>();
        assert args.size() % 2 == 0 : usageStr;
        for (int a = 0; a < args.size() / 2; a++) {
            Expression name = args.get(2 * a);
            Expression value = args.get(2 * a + 1);
            assert (name instanceof ExprConstStr) : usageStr;
            final String nameStr = ((ExprConstStr) name).getVal();
            tprintArgs.add(new TprintTuple(nameStr.substring(1, nameStr.length() - 1),
                    value));
        }
        return new ExprTprint(ctx.getCx(), this.cuda_type, tprintArgs);
    }
}