package sketch.compiler.cmdline;

import sketch.util.cli.CliAnnotatedOptionGroup;
import sketch.util.cli.CliParameter;
import sketch.util.datastructures.CmdLineHashSet;

/**
 * options which are mostly passed to cegis; a few lowering stages are in the
 * frontend though.
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you
 *          make changes, please consider contributing back!
 */
public class SolverOptions extends CliAnnotatedOptionGroup {

    private Object pTimeoutLock;

    // will be called by backend killer thread
    // to check whether it sleeps enough or not
    // this method will return additional amounts it needs to sleep
    public float checkPTimeout(float pTimeout) {
        float diff = 0;
        synchronized (pTimeoutLock) {
            if (this.pTimeout > pTimeout) {
                diff = this.pTimeout - pTimeout;
            }
        }
        return diff;
    }

    // will be called by parallel manager
    public float extendPTimeout() {
        synchronized (pTimeoutLock) {
            pTimeout = pTimeout * 2;
            // if general timeout is set, do not exceed that
            if (timeout > 0) {
                pTimeout = Math.min(pTimeout, timeout);
            }
        }
        return pTimeout;
    }

    public SolverOptions() {
        super("slv", "solver options");
        pTimeoutLock = new Object();
    }

    @CliParameter(help = "Sets the optimization level for the compiler.")
    public int olevel = -1;

    @CliParameter(help = "Seeds the random number generator. If set to zero, a random seed is used.")
    public int seed = 0;

    @CliParameter(help = "SAT solver to use for synthesis. Options: 'ABC' "
            + "for the ABC solver, 'MINI' for the MiniSat solver.")
    public SynthSolvers synth = SynthSolvers.NOT_SET;

    @CliParameter(help = "Kills the solver after given number of minutes.")
    public float timeout = 0;

    @CliParameter(help = "Kills the solver if its memory usage exceeds the bound (bytes).")
    public long memLimit = 0;

    @CliParameter(help = "Runs backend in parallel.")
    public boolean parallel = false;

    @CliParameter(help = "Kills test trials after given number of minutes.")
    public float pTimeout = (float) 1; // 1m -> 2m -> ...

    @CliParameter(help = "Number of parallel trails.")
    public int pTrials = 0;

    @CliParameter(help = "Number of cores to use.")
    public int pCPUs = 0;

    @CliParameter(help = "Concretize high-impact holes.")
    public boolean randassign = false;

    @CliParameter(help = "Degree of randomness for hole concretization.")
    public int randdegree = -1;

    @CliParameter(help = "Degrees of randomness for hole concretization.", hide_default = true, inlinesep = ",")
    public CmdLineHashSet randdegrees = new CmdLineHashSet();

    @CliParameter(help = "Number of rounds on a single back-end invocation.")
    public int ntimes = -1;

    @CliParameter(help = "Strategy for parallel-running.")
    public Strategies strategy = Strategies.NOT_SET;

    public enum Strategies {
        NOT_SET, MIN_TIME, MAX_TIME, WILCOXON
    }

    @CliParameter(help = "SAT solver to use for verification. Options: 'ABC' "
            + "for the ABC solver, 'MINI' for the MiniSat solver.")
    public VerifSolvers verif = VerifSolvers.NOT_SET;

    @CliParameter(help = "How reorder blocks should be rewritten. Options: "
            + "'exponential' to use 'insert' blocks, 'quadratic' to use a loop of switch "
            + "statements. Default value is exponential.")
    public ReorderEncoding reorderEncoding = ReorderEncoding.exponential;

    @CliParameter(help = "Helps performance on bitvector benchmarks. Avoids producing completely random inputs.")
    public boolean simpleInputs = false;

    @CliParameter(help = "Performs lightweight verification instead of full bounded verification.")
    public boolean lightverif = false;

    @CliParameter(help = "Maximum steps of random simulation to perform for every verification step.")
    public int simiters = 4;

    @CliParameter(help = "Only randomize depth holes of GUC")
    public boolean onlySpRand = false;

    @CliParameter(help = "Bias for special randomize holes")
    public int spRandBias = 1;

    @CliParameter(help = "Don't optimize using specification")
    public boolean unoptimized = false;

	@CliParameter(help = "Enable transformation of for loops based on modal changes")
	public boolean forLoopTransform = false;

	@CliParameter(help = "Set the numerical solver interaction mode")
	public String numSolverMode = "ONLY_SMOOTHING";

	@CliParameter(help = "Use snopt for the numerical solver")
	public boolean useSnopt = false;

	@CliParameter(help = "Use eager iteraction between the SAT solver and the numerical solver")
	public boolean useEager = false;

	@CliParameter(help = "Relax boolean holes with floats in range [0,1]")
	public boolean relaxBoolHoles = false;

	@CliParameter(help = "Number of times numerical solver should be run for each iteration")
	public int numTries = 1;

	@CliParameter(help = "Disable sat suggestions")
	public boolean disableSatSug = false;

	@CliParameter(help = "Disable unsat suggestions")
	public boolean disableUnsatSug = false;

	@CliParameter(help = "Conflict cutoff")
	public int conflictCutoff = 1;

	@CliParameter(help = "Number of restarts allowed with soft conflicts")
	public int maxRestarts = 10;

	@CliParameter(help = "Cost option for relaxing boolean holes")
	public int costOption = 1;

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
