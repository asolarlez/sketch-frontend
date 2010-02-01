package sketch.util.cli;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.ListIterator;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import sketch.compiler.main.PlatformLocalization;
import sketch.util.DebugOut;

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
    public final String[] args;
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

    public CliParser(String[] args, String usage, boolean errorOnUnknown) {
        super();
        this.errorOnUnknown = errorOnUnknown;
        this.args = args;
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
            cmd_line = super.parse(options, args, false);
            boolean print_help = cmd_line.hasOption("help");
            if (errorOnUnknown) {
                for (String arg : cmd_line.getArgs()) {
                    if (arg.equals("--")) {
                        break;
                    } else if (arg.startsWith("--")) {
                        DebugOut.print(String.format("unrecognized argument '%s'", arg));
                        print_help = true;
                    }
                }
            }
            if (print_help) {
                printHelpInner(options, "");
            }
        } catch (org.apache.commons.cli.ParseException e) {
            DebugOut.assertFalse(e.getMessage());
        }
        CliAnnotatedOptionGroup[] set_on_parse_arr =
                set_on_parse.toArray(new CliAnnotatedOptionGroup[0]);
        for (CliAnnotatedOptionGroup elt : set_on_parse_arr) {
            elt.set_values();
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

        // generate descriptions of the option groups
        StringBuilder description = new StringBuilder();
        description.append("\n");
        for (CliOptionGroup group : opt_groups) {
            description.append(group.prefix + " - " + group.description + "\n");
        }
        description.append(" \n");

        if (error_msg != "") {
            error_msg = "\n\n[ERROR] [SKETCH] " + error_msg;
        }

        hf.printHelp(usageStr, description.toString(), options, error_msg);
        System.exit(1); // @code standards ignore
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
        if (!hasOption && !this.errorOnUnknown) {
            if (iter.hasNext()) {
                String str = (String) iter.next();
                if (str.startsWith("-")) {
                    // got an option
                    iter.previous();
                }
            }
        } else {
            super.processOption(arg, iter);
        }
    }
}
