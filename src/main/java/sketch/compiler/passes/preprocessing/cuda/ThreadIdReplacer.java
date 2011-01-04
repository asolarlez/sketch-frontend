package sketch.compiler.passes.preprocessing.cuda;

import static sketch.util.DebugOut.assertFalse;
import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.exprs.ExprField;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.cuda.exprs.CudaThreadIdx;
import sketch.compiler.main.seq.SequentialSketchOptions;
import sketch.compiler.passes.annotations.CompilerPassDeps;
import sketch.util.cuda.CudaThreadBlockDim;

/**
 * Replace "threadIdx.x", etc. with special nodes in the AST
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
@CompilerPassDeps(runsAfter = {}, runsBefore = {})
public class ThreadIdReplacer extends FEReplacer {
    private CudaThreadBlockDim cudaBlockDim;

    public ThreadIdReplacer(final SequentialSketchOptions opts) {
        this.cudaBlockDim = opts.getCudaBlockDim();
    }

    @Override
    public Object visitExprField(ExprField exp) {
        if (exp.getLeft() instanceof ExprVar) {
            final String name = ((ExprVar) exp.getLeft()).getName();
            if (name.equals("threadIdx")) {
                return new CudaThreadIdx(exp.getCx(), exp.getName());
            } else if (name.equals("blockDim")) {
                return cudaBlockDim.blockDimConstInt(exp.getName());
            } else if (name.equals("blockIdx")) {
                assertFalse("blockIdx not supported yet");
            }  else if (name.equals("gridDim")) {
                assertFalse("gridDim not supported yet");
            }
        }
        return super.visitExprField(exp);
    }
}
