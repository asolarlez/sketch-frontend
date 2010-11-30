package sketch.compiler.cmdline;

import sketch.util.cli.CliAnnotatedOptionGroup;
import sketch.util.cli.CliParameter;

/**
 * options controlling debug printout, etc.
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
public class DebugOptions extends CliAnnotatedOptionGroup {
    public DebugOptions() {
        super("debug", "debugging");
    }

    @CliParameter(help = "Show the counterexample inputs produced by the solver. "
            + "(Equivalend to backend flag -showinputs).")
    public boolean cex = false;
    @CliParameter(help = "This flag indicates that the SAT solver should not "
            + "be invoked. Instead the frontend should look for a solution file, and "
            + "generate the code from that. It is useful when working with sketches "
            + "that take a long time to resolve if one wants to play with different "
            + "settings for code generation.")
    public boolean fakeSolver = false;
    @CliParameter(shortname = "p", help = "Show the partially evaluated code after the indicated "
            + "phase of pre or post processing.\n"
            + "'parse' for after parsing;\n"
            + "'final' for after all optimizations;\n"
            + "'lowering' for before to symbolic execution;\n"
            + "'postproc' for after partially evaluating the generated code (ugly);\n"
            + "'preproc' for after preprocessing; and\n"
            + "'taelim' for after eliminating transitive assignments (before cse, ugly).")
    public String showPhase = null;
    @CliParameter(help = "Show a trace of the symbolic execution.")
    public boolean trace = false;
    @CliParameter(shortname = "V", help = "Sets the level of verbosity for the output. 0 is "
            + "quiet mode 5 is the most verbose.")
    public int verbosity = 1;
    @CliParameter(help="Output debugging after preproc to a file")
    public String dumpPreproc = null;
}
