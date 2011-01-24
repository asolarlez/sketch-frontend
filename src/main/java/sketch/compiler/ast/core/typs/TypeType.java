package sketch.compiler.ast.core.typs;

import sketch.compiler.ast.cuda.typs.CudaMemoryType;

/**
 * type of a type. For example, if we have
 * 
 * <pre>
 * type v = typearray { \Z mod 4 -> \Z }
 * </pre>
 * 
 * the type of $v is TypeType
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
public class TypeType extends Type {
    protected Type base;

    public TypeType(CudaMemoryType memtyp) {
        super(memtyp);
        this.base = null;
    }

    public TypeType(CudaMemoryType memtyp, Type base) {
        this(memtyp);
        this.base = base;
    }

    public Type maybeGetBase() {
        return base;
    }
}
