package sketch.compiler.ast.cuda.typs;

/**
 * Cuda memory types. This should allow reusing Parameter and StmtVarDel classes
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
public enum CudaMemoryType {
    GLOBAL("Global or shared memory"), LOCAL("Thread-local variables"), UNDEFINED(
            "Code added for compatibility; these nodes should not be encountered for mem type checking passes");

    private final String description;

    private CudaMemoryType(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return description;
    }
}
