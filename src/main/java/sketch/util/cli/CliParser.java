package sketch.util.cli;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Vector;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import sketch.compiler.main.PlatformLocalization;

/**
 * parse command line with a number of option groups.
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
public class CliParser extends org.apache.commons.cli.PosixParser {
    public LinkedList<CliOptionGroup> opt_groups = new LinkedList<CliOptionGroup>();
    public LinkedList<CliAnnotatedOptionGroup> set_on_parse =
            new LinkedList<CliAnnotatedOptionGroup>();
    public CommandLine cmd_line;
    public final String[] inArgs;
    public String[] outArgs; // non-option command line entries
    public String usageStr = "[options]";
    public final boolean errorOnUnknown;

    public CliParser(String[] args) {
        this(args, null, true);
    }

    public CliParser(String[] args, String usage) {
        this(args, usage, true);
    }

    public CliParser(String[] args, boolean errorOnUnknown) {
        this(args, null, errorOnUnknown);
    }

    public CliParser(String[] inArgs, String usage, boolean errorOnUnknown) {
        super();
        this.errorOnUnknown = errorOnUnknown;
        this.inArgs = inArgs;
        if (usage != null) {
            this.usageStr = usage;
        }
    }

    protected void parse() {
        if (cmd_line != null) {
            return;
        }
        // add names
        Options options = getOptionsList();
        try {
            cmd_line = super.parse(options, inArgs, false);
            if (cmd_line.hasOption("help")) {
                printHelpInner(options, "");
            }
            Vector<String> outArgs = new Vector<String>();
            boolean tailArgs = false;
            for (String arg : cmd_line.getArgs()) {
                if (tailArgs || arg.equals("--")) {
                    outArgs.add(arg);
                    tailArgs = true;
                } else if (arg.startsWith("--")) {
                    handleUnknownLongarg(arg, null, options);
                } else if (arg.startsWith("-")) {
                    handleUnknownShortarg(arg, null, options);
                } else {
                    outArgs.add(arg);
                }
            }
            this.outArgs = outArgs.toArray(new String[0]);
        } catch (org.apache.commons.cli.ParseException e) {
            printHelpInner(options, "Apache CLI error: " + e.getMessage());
            System.exit(1); // @ code standards ignore
        }
        CliAnnotatedOptionGroup[] set_on_parse_arr =
                set_on_parse.toArray(new CliAnnotatedOptionGroup[0]);
        for (CliAnnotatedOptionGroup elt : set_on_parse_arr) {
            elt.set_values();
        }
    }

    /** default action for both short and long args */
    protected void baseHandleUnknownArg(String arg, String next, Options options) {
        if (errorOnUnknown) {
            printHelpInner(options, "unknown argument " + arg);
        } else {
            System.out.println("WARNING -- unknown argument " + arg);
        }
    }

    /** returns true if the argument $next was consumed */
    protected boolean handleUnknownLongarg(String arg, String next, Options options) {
        baseHandleUnknownArg(arg, next, options);
        return true;
    }

    /** returns true if the argument $next was consumed */
    protected boolean handleUnknownShortarg(String arg, String next, Options options) {
        baseHandleUnknownArg(arg, next, options);
        return true;
    }

    /** returns true if the argument $next was consumed */
    protected boolean handleUnknownArg(String arg, String next, Options options) {
        assert arg.startsWith("-") : "non-option";
        if (arg.startsWith("--")) {
            return handleUnknownLongarg(arg, next, options);
        } else {
            return handleUnknownShortarg(arg, next, options);
        }
    }

    public void printHelpAndExit(String error_msg) {
        printHelpInner(getOptionsList(), error_msg);
    }

    private void printHelpInner(Options options, String error_msg) {
        HelpFormatter hf = new HelpFormatter();

        // sort it by long name instead of short name
        hf.setOptionComparator(new Comparator<Option>() {
            public int compare(Option opt1, Option opt2) {
                return opt1.getLongOpt().compareTo(opt2.getLongOpt());
            }
        });

        // format the columns in the environment
        hf.setWidth(PlatformLocalization.trygetenv(90, "COLUMNS", "COLS"));

        String description = getDescription();

        if (error_msg != "") {
            error_msg = "\n\n[ERROR] [SKETCH] " + error_msg;
        }

        hf.printHelp(usageStr, description, options, error_msg);
        System.exit(1); // @code standards ignore
    }

    /** generate descriptions of the option groups */
    protected String getDescription() {
        StringBuilder description = new StringBuilder();
        description.append("\n");
        for (CliOptionGroup group : opt_groups) {
            description.append(group.prefix + " - " + group.description + "\n");
        }
        description.append(" \n");
        return description.toString();
    }

    private Options getOptionsList() {
        Options options = new Options();
        options.addOption("h", "help", false, "display help");
        for (CliOptionGroup group : opt_groups) {
            for (CliOption cmd_opt : group.opt_set.values()) {
                options.addOption(cmd_opt.as_option(group.prefix));
            }
        }
        return options;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void processOption(String arg, ListIterator iter) throws ParseException {
        boolean hasOption = getOptions().hasOption(arg);
        if (!hasOption) {
            String nextArg = null;
            if (iter.hasNext()) {
                String str = (String) iter.next();
                if (str.startsWith("-")) {
                    // got an option
                    iter.previous();
                } else {
                    nextArg = str;
                }
            }
            if (!this.handleUnknownArg(arg, nextArg, getOptions()) && (nextArg != null)) {
                iter.previous();
            }
        } else {
            super.processOption(arg, iter);
        }
    }

    public String[] getArgs() {
        assert outArgs != null : "out args null";
        return outArgs;
    }
}
