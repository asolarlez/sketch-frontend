package sketch.util.cli;

import static sketch.util.DebugOut.assertFalse;
import static sketch.util.DebugOut.print_exception;

import java.lang.reflect.Field;
import java.util.LinkedList;

public abstract class CliAnnotatedOptionGroup extends CliOptionGroup {
    CliOptionResult lazy_results;
    LinkedList<Field> fields = new LinkedList<Field>();

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
                if (!lazy_results.is_set(field.getName())) {
                    continue;
                }
                if (field.getType() == Boolean.TYPE) {
                    field.setBoolean(this, lazy_results.bool_(field.getName()));
                } else if (field.getType() == Integer.TYPE) {
                    field.setInt(this, (int) lazy_results
                            .long_(field.getName()));
                } else if (field.getType() == Long.TYPE) {
                    field.setLong(this, lazy_results.long_(field.getName()));
                } else {
                    field.set(this, lazy_results.other_type_(field.getName()));
                }
            }
        } catch (Exception e) {
            print_exception("set_values()", e);
        }
        post_set_values();
    }

    /** allow inheriting classes to check for invalid argument values */
    public void post_set_values() {
    }

    public final void add_fields() {
        if (!fields.isEmpty()) {
            assertFalse("CliAnnotatedOptionGroup -- double add fields.");
        }
        for (Field field : this.getClass().getDeclaredFields()) {
            CliParameter cli_annotation =
                    field.getAnnotation(CliParameter.class);
            if (cli_annotation != null) {
                try {
                    add("--" + field.getName(), field.get(this), cli_annotation
                            .help());
                    fields.add(field);
                } catch (Exception e) {
                    e.printStackTrace();
                    assertFalse("error accessing field", field);
                }
            }
        }
    }

    @Override
    public CliOptionResult parse(CliParser p) {
        add_fields();
        lazy_results = super.parse(p);
        p.set_on_parse.add(this);
        return lazy_results;
    }
}
