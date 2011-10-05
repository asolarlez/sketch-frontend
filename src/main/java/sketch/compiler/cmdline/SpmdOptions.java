package sketch.compiler.cmdline;

import sketch.util.cli.CliAnnotatedOptionGroup;
import sketch.util.cli.CliParameter;

/**
 * @author Zhilei Xu
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you
 *          make changes, please consider contributing back!
 */
public class SpmdOptions extends CliAnnotatedOptionGroup {
    public SpmdOptions() {
        super("spmd", "spmd sketching interface options");
    }

    @CliParameter(help = "Max NProc of spmd programs")
    public int MaxNProc = 16;
}
