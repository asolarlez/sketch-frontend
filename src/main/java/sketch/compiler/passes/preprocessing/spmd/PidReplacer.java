package sketch.compiler.passes.preprocessing.spmd;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.spmd.exprs.SpmdNProc;
import sketch.compiler.ast.spmd.exprs.SpmdPid;
import sketch.compiler.passes.annotations.CompilerPassDeps;

/**
 * Replace "threadIdx.x", etc. with special nodes in the AST
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
@CompilerPassDeps(runsAfter = {}, runsBefore = {})
public class PidReplacer extends FEReplacer {

    @Override
    public Object visitExprVar(ExprVar exp) {
        if (exp.getName().equals("spmdpid")) {
            return new SpmdPid(exp.getCx());
        } else if (exp.getName().equals("spmdnproc")) {
            return new SpmdNProc(exp.getCx());
        }
        return super.visitExprVar(exp);
    }
}
