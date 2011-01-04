package sketch.compiler.main.cuda;

import sketch.util.cli.CliAnnotatedOptionGroup;
import sketch.util.cli.CliParameter;
import sketch.util.cuda.CudaThreadBlockDim;

/**
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you
 *          make changes, please consider contributing back!
 */
public class CudaOptions extends CliAnnotatedOptionGroup {
    public CudaOptions() {
        super("cuda", "cuda sketching interface options");
    }

    @CliParameter(help = "Disable defaults (keep tmp, keep asserts)")
    public boolean noDefaults = false;
    @CliParameter(metavar = "FILE", help = "Dump the input to a file")
    public String dumpInputParse = null;
    @CliParameter(help = "Dimensions of the thread block")
    public CudaThreadBlockDim threadBlockDim = new CudaThreadBlockDim(2, 1, 1);
}
