package sketch.compiler.cmdline;

import sketch.compiler.solvers.SpinVerifier;
import sketch.util.cli.CliAnnotatedOptionGroup;
import sketch.util.cli.CliParameter;

/**
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you
 *          make changes, please consider contributing back!
 */
public class ParallelOptions extends CliAnnotatedOptionGroup {
    public ParallelOptions() {
        super("par", "parallel compiler options");
    }

    @CliParameter(help = "The length of the schedule for the parallel sections.")
    public int schedlen = 10;
    @CliParameter(help = "This is another one of those parameters that have to do with the way "
            + "things are implemented. The locks array has to be of a static size. "
            + "When you lock on a pointer, the pointer is transformed based on some "
            + "strange function, and the resulting value is used to index the lock array. "
            + "If that index is out of bounds, your sketch will not resolve, so you use this "
            + "parameter to make that lock array larger.")
    public int locklen = 10;
    @CliParameter(help = "An initial guess for how many bytes SPIN should use for its "
            + "automaton state vector.  The vector size is automatically increased "
            + "as necessary, but a good initial guess can reduce the number of calls "
            + "to SPIN.")
    public int vectorszGuess = SpinVerifier.VECTORSZ_GUESS;
    @CliParameter(help = "Do simplification on the program before running spin.")
    public boolean simplifySpin = false;
    @CliParameter(help = "This flag makes the solver really dumb. Don't use it unless you want to see how slow a dumb solver can be.")
    public boolean playDumb = false;
    @CliParameter(help = "This flag makes the solver pick candidates at random instead of doing inductive synthesis.")
    public boolean playRandom = false;
}
