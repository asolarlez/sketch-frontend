package sketch.compiler.cmdline;

import sketch.util.cli.CliAnnotatedOptionGroup;
import sketch.util.cli.CliParameter;

/**
 * Options that change the semantics of the programs. The bound options can also
 * change program semantics.
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you
 *          make changes, please consider contributing back!
 */
public class SemanticsOptions extends CliAnnotatedOptionGroup {
    public SemanticsOptions() {
        super("sem", "change program semantics");
    }

    @CliParameter(cliname = "array-OOB-policy", help = "What to do when an "
            + "array access would be out of bounds. Options: 'assertions' to fail, "
            + "'wrsilent_rdzero' to read zeros and ignore writes.")
    public ArrayOobPolicy arrayOobPolicy = ArrayOobPolicy.assertions;

    public enum ArrayOobPolicy {
        assertions, wrsilent_rdzero
    }
}
