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
    GLOBAL("Global or shared memory", "global"), LOCAL("Thread-local variables", "local"), LOCAL_TARR(
            "Thread-local variables, already converted into an array", "local-arr"), UNDEFINED(
            "Code added for compatibility; these nodes should not be encountered for mem type checking passes",
            "");

    private final String description;
    public final String syntaxName;

    private CudaMemoryType(String description, String syntaxName) {
        this.description = description;
        this.syntaxName = syntaxName;
    }

    @Override
    public String toString() {
        return description;
    }

    public String syntaxNameSpace() {
        return this.syntaxName + (this.syntaxName.isEmpty() ? "" : " ");
    }
}
