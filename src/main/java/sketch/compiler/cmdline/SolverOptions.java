package sketch.compiler.cmdline;

import sketch.util.cli.CliAnnotatedOptionGroup;
import sketch.util.cli.CliParameter;

public class SolverOptions extends CliAnnotatedOptionGroup {
    public SolverOptions() {
        super("slv", "solver options");
    }

    @CliParameter(help = "Sets the optimization level for the compiler.")
    public int olevel;
    @CliParameter(help = "Seeds the random number generator")
    public int seed;
    @CliParameter(help = "SAT solver to use for synthesis. Options: 'ABC' "
            + "for the ABC solver, 'MINI' for the MiniSat solver.")
    public SynthSolvers synth = SynthSolvers.MINI;
    @CliParameter(help = "Kills the solver after given number of minutes.")
    public float timeout;
    @CliParameter(help = "SAT solver to use for verification. Options: 'ABC' "
            + "for the ABC solver, 'MINI' for the MiniSat solver.")
    public VerifSolvers verif = VerifSolvers.MINI;

    public enum SynthSolvers {
        ABC, MINI
    }

    public enum VerifSolvers {
        ABC, MINI
    }
}
