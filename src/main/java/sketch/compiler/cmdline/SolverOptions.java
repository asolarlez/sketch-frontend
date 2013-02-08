package sketch.compiler.cmdline;

import sketch.util.cli.CliAnnotatedOptionGroup;
import sketch.util.cli.CliParameter;

/**
 * options which are mostly passed to cegis; a few lowering stages are in the
 * frontend though.
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you
 *          make changes, please consider contributing back!
 */
public class SolverOptions extends CliAnnotatedOptionGroup {
    public SolverOptions() {
        super("slv", "solver options");
    }

    
    @CliParameter(help = "Sets the optimization level for the compiler.")
    public int olevel = -1;
    @CliParameter(help = "Seeds the random number generator. If set to zero, a random seed is used.")
    public int seed;
    @CliParameter(help = "SAT solver to use for synthesis. Options: 'ABC' "
            + "for the ABC solver, 'MINI' for the MiniSat solver.")
    public SynthSolvers synth = SynthSolvers.NOT_SET;
    @CliParameter(help = "Kills the solver after given number of minutes. " +
    		"If there are minloops or a cost function, the default is 1 second")
    public float timeout;
    @CliParameter(help = "SAT solver to use for verification. Options: 'ABC' "
            + "for the ABC solver, 'MINI' for the MiniSat solver.")
    public VerifSolvers verif = VerifSolvers.NOT_SET;
    @CliParameter(help = "How reorder blocks should be rewritten. Options: "
            + "'exponential' to use 'insert' blocks, 'quadratic' to use a loop of switch "
            + "statements. Default value is exponential.")
    public ReorderEncoding reorderEncoding = ReorderEncoding.exponential;
    @CliParameter(help = "Helps performance on bitvector benchmarks. Avoids producing completely random inputs")
    public boolean simpleInputs = false;

    @CliParameter(help = "Performs lightweight verification instead of full bounded verification.")
    public boolean lightverif = false;

    public enum ReorderEncoding {
        exponential, quadratic
    }


    public enum SynthSolvers {
        NOT_SET, ABC, MINI
    }

    public enum VerifSolvers {
        NOT_SET, ABC, MINI
    }
}
