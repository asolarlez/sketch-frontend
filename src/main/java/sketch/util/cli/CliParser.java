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

    public CliParser(String[] args) {
        super();
        this.args = args;
    }

    protected void parse() {
        if (cmd_line != null) {
            return;
        }
        // add names
        Options options = new Options();
        options.addOption("h", "help", false, "display help");
        for (CliOptionGroup group : opt_groups) {
            for (CliOption cmd_opt : group.opt_set.values()) {
                options.addOption(cmd_opt.as_option(group.prefix));
            }
        }
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
                HelpFormatter hf = new HelpFormatter();
                StringBuilder description = new StringBuilder();
                description.append("\n");
                for (CliOptionGroup group : opt_groups) {
                    description.append(group.prefix + " - " + group.description
                            + "\n");
                }
                description.append(" \n");
                hf.printHelp("[options]", description.toString(), options, "");
                System.exit(1); // @code standards ignore
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
}
