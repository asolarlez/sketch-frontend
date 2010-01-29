package sketch.util.cli;

import static sketch.util.DebugOut.assertFalse;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

import sketch.util.DebugOut;

/**
 * A wrapper for a command line option, including a default value, name, and help string.
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
public final class CliOption {
    public final String name;
    public final Class<?> typ;
    public final Object defaultValue;
    public final String help;
    public final CliOptionGroup group;
    public boolean isRequired = true;
    public String metavarName;

    public CliOption(String name, Class<?> typ, Object defaultValue, String help,
            CliOptionGroup group) {
        this.name = name;
        this.typ = typ;
        this.defaultValue = defaultValue;
        this.help = help;
        this.group = group;
        if (this.name.length() > 18) {
            assertFalse("name", name, "should be shorter than 18 characters",
                    "(to prevent help description overflow)");
        } else if (this.name.contains("_")) {
            assertFalse("name", name, "should use hyphens instead of underscores",
                    "(for code standards)");
        } else if (defaultValue != null && !typ.isPrimitive() &&
                !typ.isAssignableFrom(defaultValue.getClass())) {
            assertFalse("default value", defaultValue, "of class",
                    defaultValue.getClass(), "doesn't match type", typ, "for option",
                    name);
        } else if (defaultValue instanceof Boolean) {
            if (((Boolean) defaultValue).booleanValue() != false) {
                assertFalse("boolean values must be false initially. option", name);
            }
        }
    }

    public CliOption(String name, Object defaultValue, String help, CliOptionGroup group) {
        this(name, defaultValue.getClass(), defaultValue, help, group);
    }

    public void setAdditionalInfo(final boolean isRequired, final String metavarName) {
        this.isRequired = isRequired;
        this.metavarName = metavarName;
    }

    public String full_name() {
        return group.get_prefix_with_sep() + name;
    }

    public Option as_option(String prefix) {
        boolean has_name = !(typ.equals(Boolean.class) || typ.equals(boolean.class));
        String help = this.help;
        if (defaultValue == null) {
            if (isRequired) {
                help += " (REQUIRED)";
            }
        } else if (has_name) {
            help += " [default " + defaultValue.toString() + "]";
        }
        Option result = new Option(null, full_name(), has_name, help);
        if (isRequired) {
            result.setRequired(true);
        }
        if (metavarName != null) {
            result.setArgName(metavarName);
        }
        return result;
    }

    @Override
    public String toString() {
        return String.format("CmdOption[name=%s, type=%s, default=%s, "
                + "full_name=%s, help=%s]", name, typ, defaultValue, full_name(), help);
    }

    public Object parse(CommandLine cmd_line, boolean no_defaults) {
        System.out.println(typ);
        if (CliOptional.class.isAssignableFrom(typ)) {
            if (defaultValue == null) {
                assertFalse("java generics are not stored, so you must "
                        + "provide a default value object for", this);
            }
            final CliOptional<?> defaultValue = (CliOptional<?>) this.defaultValue;
            defaultValue.setValue(getValue(defaultValue.value.getClass(), cmd_line,
                    no_defaults));
            return defaultValue;
        } else {
            return getValue(typ, cmd_line, no_defaults);
        }
    }

    public Object getValue(Class<?> typ, CommandLine cmd_line, boolean no_defaults) {
        System.out.println(typ);
        if (typmatch(typ, Boolean.class, boolean.class)) {
            return cmd_line.hasOption(full_name());
        }
        if (!cmd_line.hasOption(full_name())) {
            if (no_defaults) {
                assertFalse("CliOption - no_defaults set, but no option", this);
            }
            if (defaultValue == null) {
                DebugOut.print_colored(DebugOut.BASH_RED, "", " ", false, "argument",
                        name, "is required.\n    argument info:", this);
                System.exit(1); // @code standards ignore
            } else if (CliOptionType.class.isAssignableFrom(typ)) {
                return ((CliOptionType<?>) defaultValue).clone();
            }
            return defaultValue;
        }
        String v = cmd_line.getOptionValue(full_name());
        if (typmatch(typ, int.class, Integer.class)) {
            return Integer.parseInt(v);
        } else if (typmatch(typ, long.class, Long.class)) {
            return Long.parseLong(v);
        } else if (typmatch(typ, float.class, Float.class)) {
            return Float.parseFloat(v);
        } else if (CliOptionType.class.isAssignableFrom(typ)) {
            return ((CliOptionType<?>) defaultValue).fromString(v);
        } else if (typ.isEnum()) {
            for (Enum<?> value : (Enum<?>[]) typ.getEnumConstants()) {
                if (value.name().equals(v)) {
                    return value;
                }
            }
            assertFalse("invalid value for ", this);
            return null;
        } else {
            if (!typ.equals(String.class)) {
                DebugOut.assertFalse("can't parse option type ", typ);
            }
            return v;
        }
    }

    public boolean typmatch(Class<?> typ, Class<?>... matches) {
        for (Class<?> match : matches) {
            if (typ.equals(match)) {
                return true;
            }
        }
        return false;
    }
}
