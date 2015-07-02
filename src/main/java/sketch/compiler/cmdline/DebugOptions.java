package sketch.compiler.cmdline;

import sketch.util.cli.CliAnnotatedOptionGroup;
import sketch.util.cli.CliParameter;
import sketch.util.datastructures.CmdLineHashSet;

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
    @CliParameter(help = "Show a trace of the symbolic execution.")
    public boolean trace = false;
    @CliParameter(shortname = "V", help = "Sets the level of verbosity for the output. 0 is "
            + "quiet mode 10 is the most verbose.")
    public int verbosity = 1;

    @CliParameter(help = "Dump debug output to a file")
    public boolean dumpToFile = false;

    @CliParameter(help = "Print IR for the synthesis problem to the console.")
    public boolean showDag;

    @CliParameter(help = "Write the IR to the given file in an easy to parse format.")
    public String outputDag = null;

    @CliParameter(help = "Print names of stages and visitors as they execute")
    public boolean printPasses = false;
    @CliParameter(shortname = "p", help = "Stages / visitors to dump the program after (comma-sep)", hide_default = true, inlinesep = ",")
    public CmdLineHashSet dumpAfter = new CmdLineHashSet();
    @CliParameter(shortname = "P", help = "Stages / visitors to dump the program before (comma-sep)", hide_default = true, inlinesep = ",")
    public CmdLineHashSet dumpBefore = new CmdLineHashSet();

    @CliParameter(help = "Feed the backend input file to a bash script to check its validity")
    public String checkBackInput = null;

    /*
     * TODO
     * @CliParameter(help =
     * "Stages / visitors to show a program diff (requires `diff` tool)", inlinesep = ",")
     * public CmdLineHashSet dumpDiff = new CmdLineHashSet();
     */

    @Override
    public void post_set_values() {
        super.post_set_values();
        if (verbosity >= 5) {
            printPasses = true;
        }
    }
}
