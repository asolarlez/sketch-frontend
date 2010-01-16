package sketch.util.cli;

import static sketch.util.DebugOut.assertFalse;

import java.util.HashMap;

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

    public void addOption(String name, Class<?> typ, Object defaultValue,
            String help)
    {
        final CliOption opt =
                new CliOption(name, typ, defaultValue, help, this);
        if (opt_set.put(name, opt) != null) {
            assertFalse("already contained option", opt);
        }
    }

    public void addOption(String name, Object defaultValue, String help) {
        final CliOption opt = new CliOption(name, defaultValue, help, this);
        if (opt_set.put(name, opt) != null) {
            assertFalse("already contained option", opt);
        }
    }

    public String get_prefix_with_sep() {
        return prefix + (prefix.equals("") ? "" : "-");
    }
}
