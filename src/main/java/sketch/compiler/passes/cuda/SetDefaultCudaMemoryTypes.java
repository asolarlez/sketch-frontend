package sketch.compiler.passes.cuda;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.ast.cuda.typs.CudaMemoryType;
import sketch.compiler.passes.annotations.CompilerPassDeps;

/**
 * for .sk input files, choose appropriate default CUDA memory types
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
@CompilerPassDeps(runsAfter = {}, runsBefore = {}, debug=true)
public class SetDefaultCudaMemoryTypes extends FEReplacer {
    @Override
    public Object visitTypePrimitive(TypePrimitive t) {
        if (!t.equals(TypePrimitive.voidtype) && t.getCudaMemType() == CudaMemoryType.UNDEFINED) {
            return t.withMemType(CudaMemoryType.LOCAL);
        }
        return super.visitTypePrimitive(t);
    }
}
