package sketch.compiler.passes.preprocessing;

import sketch.compiler.ast.core.exprs.ExprTprint.CudaType;
import sketch.compiler.passes.annotations.CompilerPassDeps;

/**
 * tprint in a device function where all threads are present. only gets called once.
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
@CompilerPassDeps(runsAfter = {}, runsBefore = {})
public class AllthreadsTprintFcnCall extends TprintFcnCall {
    public AllthreadsTprintFcnCall() {
        super("tprint_allthreads");
        this.cuda_type = CudaType.Allthreads;
    }
}
