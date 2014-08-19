package sketch.compiler.passes.cuda;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.cuda.exprs.CudaBlockDim;
import sketch.compiler.main.cmdline.SketchOptions;
import sketch.util.cuda.CudaThreadBlockDim;
import sketch.util.exceptions.ExceptionAtNode;

/**
 * preprocess these to constants
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
public class ReplaceBlockDimAndGridDim extends FEReplacer {
    protected CudaThreadBlockDim cudaBlockDim;

    public ReplaceBlockDimAndGridDim(SketchOptions opts) {
        this.cudaBlockDim = new CudaThreadBlockDim(2, 1, 1); // opts.cudaOpts.threadBlockDim;
    }

    @Override
    public Object visitCudaBlockDim(CudaBlockDim cudaBlockDim) {
        if (cudaBlockDim.getIndexName().equals("x")) {
            return new ExprConstInt(cudaBlockDim, this.cudaBlockDim.x);
        } else if (cudaBlockDim.getIndexName().equals("y")) {
            return new ExprConstInt(cudaBlockDim, this.cudaBlockDim.y);
        } else if (cudaBlockDim.getIndexName().equals("z")) {
            return new ExprConstInt(cudaBlockDim, this.cudaBlockDim.z);
        } else {
            throw new ExceptionAtNode("invalid index name", cudaBlockDim);
        }
    }
}
