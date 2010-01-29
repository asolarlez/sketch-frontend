package sketch.util.cli;

import static sketch.util.DebugOut.assertFalse;
import static sketch.util.DebugOut.print_exception;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.LinkedList;

public abstract class CliAnnotatedOptionGroup extends CliOptionGroup {
    CliOptionResult lazy_results;
    LinkedList<Field> fields = new LinkedList<Field>();
    public HashSet<String> cmdlineSetFields = new HashSet<String>();

    public CliAnnotatedOptionGroup(String prefix, String description) {
        super(prefix, description);
    }

    public final void set_values() {
        // don't call this recursively
        if (!lazy_results.parser.set_on_parse.remove(this)) {
            return;
        }
        try {
            for (Field field : fields) {
                final String name = cmdlineName(field);
                if (!lazy_results.is_set(name)) {
                    continue;
                }
                cmdlineSetFields.add(name);
                // NOTE -- this is only for primitive types, we don't need to do a lot of
                // type checking.
                final Class<?> typ = field.getType();
                if (typ == Boolean.TYPE) {
                    field.setBoolean(this, lazy_results.bool_(name));
                } else if (typ == Integer.TYPE) {
                    field.setInt(this, (int) lazy_results.int_(name));
                } else if (typ == Long.TYPE) {
                    field.setLong(this, lazy_results.long_(name));
                } else if (typ == String.class) {
                    field.set(this, lazy_results.str_(name));
                } else {
                    field.set(this, lazy_results.get_value(name));
                }
            }
        } catch (Exception e) {
            print_exception("set_values()", e);
        }
        post_set_values();
    }

    /** allow inheriting classes to check for invalid argument values */
    public void post_set_values() {}

    public final void add_fields() {
        if (!fields.isEmpty()) {
            assertFalse("CliAnnotatedOptionGroup -- double add fields.");
        }
        for (Field field : this.getClass().getDeclaredFields()) {
            CliParameter cli_annotation = field.getAnnotation(CliParameter.class);
            if (cli_annotation != null) {
                try {
                    final CliOption opt =
                            new CliOption(cmdlineName(field), field.getType(),
                                    field.get(this), cli_annotation.help(), this);
                    opt.setAdditionalInfo(cli_annotation.required(),
                            cli_annotation.metavar(), cli_annotation.inlinesep());
                    addOption(opt);
                    fields.add(field);
                } catch (Exception e) {
                    e.printStackTrace();
                    assertFalse("error accessing field", field);
                }
            }
        }
    }

    public static String cmdlineName(Field field) {
        CliParameter cli_annotation = field.getAnnotation(CliParameter.class);
        if (cli_annotation.cliname() != null && !cli_annotation.cliname().equals("")) {
            return cli_annotation.cliname();
        }
        String name = field.getName();
        return name.replaceAll("([a-z])([A-Z])", "$1-$2").toLowerCase().replace('_', '-');
    }

    @Override
    public CliOptionResult parse(CliParser p) {
        add_fields();
        lazy_results = super.parse(p);
        p.set_on_parse.add(this);
        return lazy_results;
    }
}
