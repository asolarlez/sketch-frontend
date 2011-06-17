package sketch.compiler.test;

import java.io.File;

import static sketch.compiler.main.PlatformLocalization.path;

/**
 * Filter files in a directory
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
public class BasicFileFilter {
    public final File directory;
    public final String prefix;
    public final String suffix;

    public BasicFileFilter(File directory, String prefix, String suffix) {
        this.directory = directory;
        this.prefix = prefix;
        this.suffix = suffix;
    }

    public static BasicFileFilter miniTests(String base, String... path) {
        return new BasicFileFilter(path(base, path), "miniTest", "sk");
    }

    public static BasicFileFilter sketches(String base, String... path) {
        return new BasicFileFilter(path(base, path), "", "sk");
    }
}
