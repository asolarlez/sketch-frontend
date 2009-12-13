package sketch.util.cli;

import static sketch.util.DebugOut.BASH_RED;
import static sketch.util.DebugOut.assertFalse;
import static sketch.util.DebugOut.print_colored;

import java.util.HashMap;

import sketch.util.DebugOut;

/**
 * a group of options. subclasses are initialized by making calls in the
 * constructor; see e.g. ScSynthesisOptions for an example.
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you
 *          make changes, please consider contributing back!
 */
public abstract class CliOptionGroup {
    protected HashMap<String, CliOption> opt_set =
            new HashMap<String, CliOption>();
    protected String prefix;
    protected String description;

    public CliOptionGroup(String prefix, String description) {
        this.prefix = prefix;
        this.description = description;
    }

    public CliOptionGroup() {
        prefix = "";
        description =
                "[default prefix] options to " + this.getClass().getName();
    }

    public CliOptionResult parse(CliParser p) {
        p.opt_groups.add(this);
        return new CliOptionResult(this, p);
    }

    public void add(Object... options) {
        CliOption opt = new CliOption();
        for (Object ent : options) {
            if (ent instanceof String) {
                String as_str = (String) ent;
                if (as_str.startsWith("--")) {
                    if (as_str.contains("num") || as_str.contains("len")) {
                        opt.type_ = Long.class;
                        if (opt.default_.equals(new Boolean(false))) {
                            opt.default_ = null;
                        }
                    }
                    if (opt.name_ != null) {
                        DebugOut.assertFalse("name already exists", opt,
                                options);
                    }
                    opt.name_ = as_str.substring(2);
                } else if (opt.help_ == null) {
                    opt.help_ = as_str;
                } else {
                    // middle argument (default value) type string, just got
                    // help, so shift args
                    opt.type_ = String.class;
                    opt.default_ = opt.help_;
                    opt.help_ = as_str;
                }
            } else if (ent instanceof Long) {
                opt.type_ = Long.class;
                opt.default_ = ent;
            } else if (ent instanceof Integer) {
                opt.type_ = Long.class;
                opt.default_ = new Long(((Integer) ent).longValue());
            } else if (ent instanceof Float) {
                opt.type_ = Float.class;
                opt.default_ = ent;
            } else if (ent instanceof Class<?>) {
                opt.type_ = (Class<?>) ent;
                opt.default_ = null;
            } else if (ent instanceof CliOptionType<?>) {
                opt.type_ = ent.getClass();
                opt.default_ = ent;
            } else if (ent instanceof Boolean) {
                if (((Boolean) ent).booleanValue() != false) {
                    assertFalse("boolean values must be false initially.");
                }
            } else {
                print_colored(BASH_RED, "[assert info]", " ", true, options);
                assertFalse("unknown argument type", ent);
            }
        }
        if (opt.name_ == null) {
            assertFalse("no name given", options);
        }
        if (opt.default_ != null
                && !opt.type_.isAssignableFrom(opt.default_.getClass()))
        {
            assertFalse("default value", opt.default_, "doesn't match type",
                    opt.type_, "; options", options);
        }
        if (opt_set.put(opt.name_, opt) != null) {
            assertFalse("already contained option", opt);
        }
    }
}
