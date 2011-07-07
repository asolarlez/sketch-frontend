package sketch.util.cli;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.util.Vector;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

import sketch.util.DebugOut;

import static sketch.util.DebugOut.assertFalse;

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
    public boolean isRequired = false;
    public String metavarName;
    public String inlinesep;
    public String shortname;
    protected boolean hideDefault;

    public CliOption(String name, Class<?> typ, Object defaultValue, String help,
            CliOptionGroup group)
    {
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
                !typ.isAssignableFrom(defaultValue.getClass()))
        {
            assertFalse("default value", defaultValue, "of class",
                    defaultValue.getClass(), "doesn't match type", typ, "for option",
                    name);
        } else if (defaultValue instanceof Boolean) {
            if (((Boolean) defaultValue).booleanValue() != false) {
                assertFalse("boolean values must be false initially. option", name);
            }
        }
    }

    public CliOption(String name, Object defaultValue, String help, CliOptionGroup group)
    {
        this(name, defaultValue.getClass(), defaultValue, help, group);
    }

    public void setAdditionalInfo(final boolean isRequired, final String metavarName,
            final String inlinesep, String shortname, boolean hideDefault)
    {
        this.isRequired = isRequired;
        this.hideDefault = hideDefault;
        this.metavarName = metavarName;
        this.inlinesep = inlinesep;
        this.shortname = shortname;
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
        } else if (has_name && !hideDefault) {
            help += " [default " + defaultValue.toString() + "]";
        }
        if (shortname != null && shortname.equals("")) {
            shortname = null;
        }
        Option result = new Option(shortname, full_name(), has_name, help);
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
        if (CliOptional.class.isAssignableFrom(typ)) {
            if (defaultValue == null) {
                assertFalse("java generics are not stored, so you must "
                        + "provide a default value object for", this);
            }
            final CliOptional<?> defaultValue = (CliOptional<?>) this.defaultValue;
            defaultValue.setValue(getValue(defaultValue.value.getClass(), cmd_line,
                    no_defaults));
            return defaultValue;
        } else if (typ.isArray()) {
            final Class<?> subtyp = typ.getComponentType();
            if (!cmd_line.hasOption(full_name())) {
                return defaultValue;
            } else {
                final Object[] values = getValues(subtyp, cmd_line, no_defaults);
                if (!inlinesep.equals("")) {
                    assert (subtyp.equals(String.class));
                    Vector<String> result = new Vector<String>();
                    for (Object value : values) {
                        for (String entry : ((String) value).split(inlinesep)) {
                            result.add(entry);
                        }
                    }
                    return result.toArray(new String[0]);
                }
                final Object result = Array.newInstance(subtyp, values.length);
                System.arraycopy(values, 0, result, 0, values.length);
                return result;
            }
        } else {
            return getValue(typ, cmd_line, no_defaults);
        }
    }

    public Object getValue(Class<?> typ, CommandLine cmd_line, boolean no_defaults) {
        final Object[] values = getValues(typ, cmd_line, no_defaults);
        if (values.length != 1) {
            System.err.println(String.format(
                    "NOTE -- multiple values provided for command line option '%s'; using '%s'",
                    this.full_name(), values[values.length - 1]));
        }
        return values[values.length - 1];
    }

    public Object[] getValues(Class<?> typ, CommandLine cmd_line, boolean no_defaults) {
        if (typmatch(typ, Boolean.class, boolean.class)) {
            return new Object[] { cmd_line.hasOption(full_name()) };
        }
        if (!cmd_line.hasOption(full_name())) {
            if (no_defaults) {
                assertFalse("CliOption - no_defaults set, but no option", this);
            }
            if (defaultValue == null) {
                DebugOut.print_stderr_colored(DebugOut.BASH_RED, "", " ", false, "argument",
                        name, "is required.\n    argument info:", this);
                System.exit(1); // @code standards ignore
            } else if (CliOptionType.class.isAssignableFrom(typ)) {
                return new Object[] { ((CliOptionType<?>) defaultValue).clone() };
            }
            return new Object[] { defaultValue };
        }
        Vector<Object> result_vector = new Vector<Object>();
        optionloop: for (String v : cmd_line.getOptionValues(full_name())) {
            if (typmatch(typ, int.class, Integer.class)) {
                result_vector.add(Integer.parseInt(v));
            } else if (typmatch(typ, long.class, Long.class)) {
                result_vector.add(Long.parseLong(v));
            } else if (typmatch(typ, float.class, Float.class)) {
                result_vector.add(Float.parseFloat(v));
            } else if (CliOptionType.class.isAssignableFrom(typ)) {
                result_vector.add(((CliOptionType<?>) defaultValue).fromString(v));
            } else if (typ.isEnum()) {
                for (Enum<?> value : (Enum<?>[]) typ.getEnumConstants()) {
                    if (value.name().equals(v)) {
                        result_vector.add(value);
                        continue optionloop;
                    }
                }
                assertFalse("invalid value ", v, " for ", this);
                return null;
            } else {
                if (!typ.equals(String.class)) {
                    Constructor<?> constr;
                    try {
                        constr = typ.getConstructor(String.class);
                        result_vector.add(constr.newInstance(v));
                    } catch (NoSuchMethodException e) {
                        assertFalse("Custom class", typ.getName(), " for parameter ",
                                full_name(), " doesn't have a new(string) constructor");
                        e.printStackTrace();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    result_vector.add(v);
                }
            }
        }
        return result_vector.toArray();
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
