package sketch.util.cli;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(value = RetentionPolicy.RUNTIME)
public @interface CliParameter {
    String help();
    String cliname() default "";
    boolean required() default false;
    String metavar() default "";
    String inlinesep() default "";
}
