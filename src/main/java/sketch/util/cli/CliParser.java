package sketch.util.cli;

import java.util.LinkedList;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

import sketch.util.DebugOut;

/**
 * parse command line with a number of option groups.
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you
 *          make changes, please consider contributing back!
 */
public class CliParser extends org.apache.commons.cli.PosixParser {
    public LinkedList<CliOptionGroup> opt_groups =
            new LinkedList<CliOptionGroup>();
    public LinkedList<CliAnnotatedOptionGroup> set_on_parse =
            new LinkedList<CliAnnotatedOptionGroup>();
    public CommandLine cmd_line;
    public String[] args;
    public String usageStr = "[options]";

    public CliParser(String[] args) {
        super();
        this.args = args;
    }
    
    public CliParser(String[] args, String usage) {
        super();
        this.args = args;
        this.usageStr = usage;
    }

    protected void parse() {
        if (cmd_line != null) {
            return;
        }
        // add names
        Options options = getOptionsList();
        try {
            cmd_line = super.parse(options, args, true);
            boolean print_help = cmd_line.hasOption("help");
            for (String arg : cmd_line.getArgs()) {
                if (arg.equals("--")) {
                    break;
                } else if (arg.startsWith("--")) {
                    DebugOut.print("unrecognized argument", arg);
                    print_help = true;
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
        String cols = System.getenv("COLUMNS");
        if (cols != null && !cols.equals("")) {
            try {
                hf.setWidth(Integer.parseInt(cols));
            } catch (Exception e) {
            }
        } else {
            hf.setWidth(100);
        }
        StringBuilder description = new StringBuilder();
        description.append("\n");
        for (CliOptionGroup group : opt_groups) {
            description.append(group.prefix + " - " + group.description
                    + "\n");
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
}
