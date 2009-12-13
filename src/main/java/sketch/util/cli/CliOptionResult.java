package sketch.util.cli;

import java.util.HashMap;

import sketch.util.DebugOut;

/**
 * returns values for command line options. this is lazy in the sense that the
 * command line will only be parsed when the first value is requested.
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you
 *          make changes, please consider contributing back!
 */
public class CliOptionResult {
    CliOptionGroup options;
    CliParser parser;
    protected HashMap<String, Object> cached_results;
    public boolean no_defaults = false;

    public CliOptionResult(CliOptionGroup options, CliParser parser) {
        this.options = options;
        this.parser = parser;
        if (options == null || parser == null) {
            DebugOut.assertFalse();
        }
        cached_results = new HashMap<String, Object>();
    }

    protected Object get_value(String name) {
        parser.parse();
        if (cached_results == null) {
            DebugOut.assertFalse();
        }
        Object result = cached_results.get(name);
        if (result == null) {
            if (options.opt_set == null) {
                DebugOut.assertFalse();
            }
            CliOption opt = options.opt_set.get(name);
            if (opt == null) {
                DebugOut.assertFalse("invalid name", name);
            }
            result = opt.parse(parser.cmd_line, no_defaults);
            cached_results.put(name, result);
        }
        return result;
    }

    public boolean is_set(String name) {
        parser.parse();
        CliOption opt = options.opt_set.get(name);
        return parser.cmd_line.hasOption(opt.full_name_);
    }

    public boolean bool_(String name) {
        return (Boolean) get_value(name);
    }

    public String str_(String name) {
        return (String) get_value(name);
    }

    public long long_(String name) {
        return (Long) get_value(name);
    }

    public float flt_(String name) {
        return (Float) get_value(name);
    }

    @SuppressWarnings("unchecked")
    public <T extends CliOptionType> T other_type_(String name) {
        return (T) get_value(name);
    }
}
