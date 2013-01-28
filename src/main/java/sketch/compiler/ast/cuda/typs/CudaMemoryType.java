package sketch.compiler.ast.cuda.typs;

/**
 * Indicates whether a
 * <code>Type<code> sits in local memory (thus each processor has its own copy) or in global memory (thus there is only one copy). 
 * A special case is LOCAL_TARR which is produced by SPMD transformation, to indicate that a LOCAL type already transformed to an array which contains a copy for each processor.
 * 
 * <p>
 * Originally this is used by Cuda sketch, but the Cuda sketch itself is no longer supported. Now the SPMD sketch re-uses this.
 * 
 * <p>
 * Note: should be renamed to "SpmdMemoryType", or just "MemoryType", to remove the dependency on Cuda things.
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

    public boolean isLocalOrUndefined() {
        return (this == LOCAL) || (this == UNDEFINED);
    }

    @Override
    public String toString() {
        return description;
    }

    public String syntaxNameSpace() {
        return this.syntaxName + (this.syntaxName.isEmpty() ? "" : " ");
    }
}
