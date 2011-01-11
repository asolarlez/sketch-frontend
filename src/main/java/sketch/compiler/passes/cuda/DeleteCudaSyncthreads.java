package sketch.compiler.passes.cuda;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.cuda.stmts.CudaSyncthreads;
import sketch.compiler.passes.annotations.CompilerPassDeps;

/**
 * delete syncthreads
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
@CompilerPassDeps(runsBefore = {}, runsAfter = {})
public class DeleteCudaSyncthreads extends FEReplacer {
    @Override
    public Object visitCudaSyncthreads(CudaSyncthreads cudaSyncthreads) {
        return null;
    }
}
