package sketch.compiler.passes.cleanup;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.exprs.ExprTprint;
import sketch.compiler.passes.annotations.CompilerPassDeps;

/**
 * remove tprint statements from intermediate programs
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
@CompilerPassDeps(runsAfter = {}, runsBefore = {}, debug = true)
public class RemoveTprint extends FEReplacer {
    @Override
    public Object visitExprTprint(ExprTprint exprTprint) {
        return null;
    }
}
