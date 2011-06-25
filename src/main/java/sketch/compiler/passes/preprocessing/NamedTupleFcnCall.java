package sketch.compiler.passes.preprocessing;

import java.util.List;

import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprType;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.typs.NamedTupleType;

/**
 * type constructor for a named tuple
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
public class NamedTupleFcnCall extends BuiltinFcnCall {
    public NamedTupleFcnCall() {
        super("namedtuple", -1);
    }

    @Override
    public Object builtinReplacement(ExprFunCall call, List<Expression> args) {
        return new ExprType(call, new NamedTupleType(args));
    }
}
